package io.xpipe.ext.proc;

import com.jcraft.jsch.*;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.ext.proc.augment.SshCommandAugmentation;
import io.xpipe.ext.proc.util.ShellHelper;
import io.xpipe.ext.proc.util.SshToolHelper;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ScriptHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Predicate;

public class SshProcessControlImpl extends ShellProcessControlImpl {

    public static final int TIMEOUT = 5000;

    private final SshStore store;
    protected boolean stdinClosed;
    private Session session;
    private Channel channel;
    private InputStream stdout;
    private InputStream stderr;
    private OutputStream stdin;

    public SshProcessControlImpl(SshStore store) {
        this.store = store;
        this.elevationPassword = store.getPassword();
    }

    @Override
    public String prepareTerminalOpen() throws Exception {
        return prepareIntermediateTerminalOpen(null);
    }

    public void closeStdin() throws IOException {
        if (stdinClosed) {
            return;
        }

        stdinClosed = true;
        getStdin().flush();
        getStdin().close();
    }

    @Override
    public boolean isStdinClosed() {
        return stdinClosed;
    }

    @Override
    public void close() throws IOException {
        exitAndWait();
    }

    @Override
    public void kill() throws Exception {
        exitAndWait();
    }

    public void restart() throws Exception {
        close();
        start();
    }

    @Override
    public String prepareIntermediateTerminalOpen(String content) throws Exception {
        String script = null;
        if (content != null) {
            try (var pc = start()) {
                script = ScriptHelper.createExecScript(pc, content, false);
            }
        }

        try (var pc = store.getProxy().create().start()) {
            var command = SshToolHelper.toCommand(store, pc);
            var augmentedCommand = new SshCommandAugmentation().prepareTerminalCommand(pc, command, script);
            var passwordCommand = SshToolHelper.passPassword(
                    augmentedCommand,
                    store.getPassword(),
                    store.getKey() != null ? store.getKey().getPassword() : null,
                    pc);

            var operator = pc.getShellType().getOrConcatenationOperator();
            var consoleCommand = passwordCommand + operator + pc.getShellType().getPauseCommand();

            return pc.prepareIntermediateTerminalOpen(consoleCommand);
        }
    }

    private Session createSession() throws JSchException {
        var j = new JSch();
        if (store.getKey() != null) {
            if (store.getKey().password != null) {
                j.addIdentity(
                        store.getKey().file.toString(), store.getKey().password.getSecretValue());
            } else {
                j.addIdentity(store.getKey().file.toString());
            }
        }

        var session = j.getSession(store.getUser(), store.getHost(), store.getPort());
        if (store.getPassword() != null) {
            session.setPassword(store.getPassword().getSecretValue());
        }

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.setDaemonThread(true);
        session.setTimeout(TIMEOUT);
        return session;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public ShellProcessControl elevated(Predicate<ShellProcessControl> elevationFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ShellProcessControl start() throws Exception {
        if (running) {
            return this;
        }

        session = createSession();
        TrackEvent.withTrace("ssh", "Opening ssh connection")
                .tag("host", session.getHost())
                .tag("port", session.getPort())
                .tag("user", session.getUserName())
                .handle();

        session.connect();
        channel = session.openChannel("shell");
        ((ChannelShell) channel).setPty(false);
        channel.connect();
        stdout = channel.getInputStream();
        stderr = InputStream.nullInputStream();
        stdin = channel.getOutputStream();
        stdinClosed = false;

        running = true;
        shellType = ShellHelper.determineType(this, null, null, null, startTimeout);
        charset = shellType.determineCharset(this);
        osType = ShellHelper.determineOsType(this);
        channel.disconnect();

        TrackEvent.withTrace("proc", "Detected shell environment...")
                .tag("shellType", shellType.getName())
                .tag("charset", charset.name())
                .handle();

        channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(getShellType().getNormalOpenCommand());
        ((ChannelExec) channel).setPty(false);
        stdout = channel.getInputStream();
        stderr = ((ChannelExec) channel).getErrStream();
        stdin = channel.getOutputStream();
        channel.connect();

        // Execute optional init commands
        for (String s : initCommands) {
            executeLine(s);
        }

        return this;
    }

    @Override
    public void exitAndWait() throws IOException {
        if (!running) {
            return;
        }

        TrackEvent.withTrace("proc", "Exiting ssh control ...").handle();

        if (channel != null) {
            channel.disconnect();
            channel = null;
        }
        session.disconnect();
        session = null;
        running = false;
        stdinClosed = true;

        if (!PrefsProvider.get(ProcPrefs.class).enableCaching().get()) {
            shellType = null;
            charset = null;
            command = null;
        }

        getStderr().close();
        getStdin().close();
        getStdout().close();
    }

    @Override
    public InputStream getStdout() {
        return stdout;
    }

    @Override
    public OutputStream getStdin() {
        return stdin;
    }

    @Override
    public InputStream getStderr() {
        return stderr;
    }
}
