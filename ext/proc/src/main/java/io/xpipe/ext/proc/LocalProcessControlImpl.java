package io.xpipe.ext.proc;

import io.xpipe.core.impl.LocalProcessControlProvider;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.process.ShellTypes;
import io.xpipe.ext.proc.util.ShellHelper;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.prefs.PrefsProvider;
import io.xpipe.extension.util.ScriptHelper;
import org.apache.commons.exec.CommandLine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class LocalProcessControlImpl extends ShellProcessControlImpl {

    private static final int EXIT_TIMEOUT = 5000;
    protected boolean stdinClosed;
    private Process process;

    @Override
    public String prepareTerminalOpen() throws Exception {
        return prepareIntermediateTerminalOpen(null);
    }

    public void closeStdin() throws IOException {
        if (stdinClosed) {
            return;
        }

        stdinClosed = true;
        getStdin().close();
    }

    @Override
    public boolean isStdinClosed() {
        return stdinClosed;
    }

    @Override
    public ShellType getShellType() {
        return ShellTypes.getPlatformDefault();
    }

    @Override
    public void close() throws IOException {
        TrackEvent.withTrace("proc", "Closing local shell ...").handle();
        exitAndWait();
    }

    @Override
    public void exitAndWait() throws IOException {
        if (!running) {
            return;
        }

        if (!isStdinClosed()) {
            writeLine(shellType.getExitCommand());
        }

        getStdout().close();
        getStderr().close();
        getStdin().close();

        stdinClosed = true;
        shellType = null;
        charset = null;
        uuid = null;
        command = null;

        try {
            process.waitFor(EXIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }

        running = false;
    }

    @Override
    public void kill() throws IOException {
        TrackEvent.withTrace("proc", "Killing local shell ...").handle();

        process.destroyForcibly();
        // Don't close stout as that might hang too in case it is frozen
        // getStdout().close();
        // getStderr().close();
        getStdin().close();

        running = false;
    }

    public void restart() throws Exception {
        close();
        start();
    }

    @Override
    public String prepareIntermediateTerminalOpen(String content) throws Exception {
        try (var pc = start()) {
            var file = ScriptHelper.constructOpenWithInitScriptCommand(pc, initCommands, content);
            TrackEvent.withDebug("proc", "Writing open init script")
                    .tag("file", file)
                    .tag("content", content)
                    .handle();

            return file;
        }
    }

    @Override
    public boolean isLocal() {
        return true;
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

        var localType = ShellTypes.getPlatformDefault();
        command = localType.getNormalOpenCommand();
        uuid = UUID.randomUUID();

        TrackEvent.withTrace("proc", "Starting local process")
                .tag("command", command)
                .handle();
        var parsed = CommandLine.parse(command);
        var args = new ArrayList<String>();
        args.add(parsed.getExecutable());
        args.addAll(List.of(parsed.getArguments()));
        process = Runtime.getRuntime().exec(args.toArray(String[]::new));
        stdinClosed = false;
        running = true;
        shellType = ShellHelper.determineType(this, Charset.defaultCharset(), command, null, startTimeout);
        charset = shellType.determineCharset(this);
        osType = OsType.getLocal();

        for (String s : initCommands) {
            executeLine(s);
        }

        return this;
    }

    @Override
    public InputStream getStdout() {
        return process.getInputStream();
    }

    @Override
    public OutputStream getStdin() {
        return process.getOutputStream();
    }

    @Override
    public InputStream getStderr() {
        return process.getErrorStream();
    }

    public static class Provider extends LocalProcessControlProvider {

        @Override
        public ShellProcessControl createProcessControl() {
            return new LocalProcessControlImpl();
        }

        @Override
        public void openInTerminal(String title, String command) throws Exception {
            var type = PrefsProvider.get(ProcPrefs.class).terminalType().getValue();
            type.launch(title, command);
        }
    }
}
