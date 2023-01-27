package io.xpipe.ext.proc;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.ext.proc.util.ElevationHelper;
import io.xpipe.ext.proc.util.ShellHelper;
import io.xpipe.ext.proc.util.ShellReader;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.ScriptHelper;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class SubShellProcessControlImpl extends ShellProcessControlImpl {

    private final ShellProcessControl parent;

    @NonNull
    private final Function<ShellProcessControl, String> commandFunction;

    private final BiFunction<ShellProcessControl, String, String> terminalCommand;
    private boolean manageParent;
    private Predicate<ShellProcessControl> elevationFunction = shellProcessControl -> false;

    public SubShellProcessControlImpl(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> commandFunction,
            BiFunction<ShellProcessControl, String, String> terminalCommand) {
        this.commandFunction = commandFunction;
        this.parent = parent;
        this.manageParent = !parent.isRunning();
        this.terminalCommand = terminalCommand;
    }

    @Override
    public void kill() throws Exception {
        TrackEvent.withTrace("proc", "Killing sub shell").tag("name", command).handle();
        parent.kill();
        running = false;
    }

    @Override
    public String prepareTerminalOpen(String content) throws Exception {
        if (this.terminalCommand == null) {
            throw new UnsupportedOperationException("Terminal open not supported");
        }

        if (content == null) {
            if (isRunning()) {
                exitAndWait();
            }

            try (var ignored = parent.start()) {
                var operator = parent.getShellType().getOrConcatenationOperator();
                var consoleCommand = this.terminalCommand.apply(parent, null);
                var elevated = elevationFunction.test(parent);
                if (elevated) {
                    consoleCommand = ElevationHelper.elevateTerminalCommand(consoleCommand, parent);
                }
                var openCommand =
                        consoleCommand + operator + parent.getShellType().getPauseCommand();
                return parent.prepareTerminalOpen(openCommand);
            }
        } else {
            try (var ignored = start()) {
                var operator = parent.getShellType().getOrConcatenationOperator();
                var file = ScriptHelper.createExecScript(this, content, false);

                var consoleCommand = this.terminalCommand.apply(parent, file);
                var elevated = elevationFunction.test(parent);
                if (elevated) {
                    consoleCommand = ElevationHelper.elevateTerminalCommand(consoleCommand, parent);
                }
                var openCommand =
                        consoleCommand + operator + parent.getShellType().getPauseCommand();

                TrackEvent.withTrace("proc", "Preparing for console open")
                        .tag("file", file)
                        .tag("content", ShellHelper.censor(content, sensitive))
                        .tag("openCommand", openCommand)
                        .handle();

                exitAndWait();
                parent.restart();
                return parent.prepareTerminalOpen(openCommand);
            }
        }
    }

    public void restart() throws Exception {
        var oldValue = manageParent;
        manageParent = true;
        close();
        start();
        manageParent = oldValue;
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public ShellProcessControl elevated(Predicate<ShellProcessControl> elevationFunction) {
        this.elevationFunction = elevationFunction;
        return this;
    }

    @Override
    public ShellProcessControl start() throws Exception {
        if (running) {
            return this;
        }

        parent.start();
        TrackEvent.withTrace("proc", "Starting sub shell ...")
                .tag("manageParent", manageParent)
                .handle();

        uuid = UUID.randomUUID();
        command = commandFunction.apply(parent);

        try {
            startup();
        } catch (Throwable t) {
            kill();
            throw t;
        }

        return this;
    }

    private void startup() throws Exception {
        var elevated = elevationFunction.test(this);

        var startId = UUID.randomUUID();
        var commandToExecute = String.format(
                "%s%s %s%s %s%s %s%s %s",
                parent.getShellType().getEchoCommand(startId.toString(), false),
                parent.getShellType().getConcatenationOperator(),
                parent.getShellType().getEchoCommand(startId.toString(), true),
                parent.getShellType().getConcatenationOperator(),
                command,
                parent.getShellType().getConcatenationOperator(),
                parent.getShellType().getEchoCommand(uuid.toString(), false),
                parent.getShellType().getConcatenationOperator(),
                parent.getShellType().getEchoCommand(uuid.toString(), true));

        TrackEvent.withTrace("proc", "Executing sub shell command...")
                .tag("command", command)
                .tag("elevated", elevated)
                .handle();

        if (elevated) {
            commandToExecute = ElevationHelper.elevateNormalCommand(commandToExecute, parent, command);
        }
        parent.executeCommand(commandToExecute);

        // Wait for prefix output
        // In case this fails, we know that the whole command has not been executed, which can only happen if the syntax ever occurred
        var stdoutPre = ShellReader.readErrorUntilOccurrenceOrTimeout(parent.getStdout(), parent.getCharset(), startId.toString(), 1000);
        var stderrPre = ShellReader.readErrorUntilOccurrenceOrTimeout(parent.getStderr(), parent.getCharset(), startId.toString(), 1000);
        if (stdoutPre != null || stderrPre != null) {
            throw new IOException("Command syntax error" + (stderrPre != null && stderrPre.trim().length() > 0 ? ": " + stderrPre : ""));
        }

        running = true;
        shellType =
                ShellHelper.determineType(this, parent.getCharset(), commandToExecute, uuid.toString(), startTimeout);
        shellType.disableHistory(this);
        charset = shellType.determineCharset(this);
        osType = ShellHelper.determineOsType(this);

        TrackEvent.withTrace("proc", "Detected shell environment...")
                .tag("shellType", shellType.getName())
                .tag("charset", charset.name())
                .tag("osType", osType.getName())
                .handle();

        // Read all output until now
        executeCommand(getShellType().getEchoCommand(uuid.toString(), false)
                + getShellType().getConcatenationOperator()
                + getShellType().getEchoCommand(uuid.toString(), true));
        ShellReader.readUntilOccurrence(getStdout(), getCharset(), uuid.toString(), commandToExecute, false);
        var readError = ShellReader.readUntilOccurrence(getStderr(), getCharset(), uuid.toString(), null, false);
        if (!readError.isEmpty()) {
            // Not every stderr output is actually a proper error, most of the time it's just warnings
            TrackEvent.withWarn("proc", readError).handle();
            // throw new ProcessOutputException(readError);
        }
    }

    @Override
    public void closeStdin() throws IOException {
        parent.closeStdin();
    }

    @Override
    public boolean isStdinClosed() {
        return parent.isStdinClosed();
    }

    @Override
    public void close() throws IOException {
        TrackEvent.withTrace("proc", "Closing sub shell ...")
                .tag("manageParent", manageParent)
                .handle();

        exitAndWait();
        if (manageParent) {
            parent.close();
        }
    }

    @Override
    public InputStream getStdout() {
        return parent.getStdout();
    }

    @Override
    public OutputStream getStdin() {
        return parent.getStdin();
    }

    @Override
    public InputStream getStderr() {
        return parent.getStderr();
    }

    @Override
    public void exitAndWait() throws IOException {
        if (running) {
            TrackEvent.withTrace("proc", "Exiting sub shell ...")
                    .tag("type", getShellType().getName())
                    .tag("command", command)
                    .tag("id", uuid)
                    .handle();

            if (!isStdinClosed()) {
                writeLine(shellType.getExitCommand());

                ShellReader.readUntilOccurrence(parent.getStderr(), parent.getCharset(), uuid.toString(), null, false);
                ShellReader.readUntilOccurrence(parent.getStdout(), parent.getCharset(), uuid.toString(), null, false);
            }

            running = false;
        }

        shellType = null;
        charset = null;
        uuid = null;
        command = null;
    }
}
