package io.xpipe.ext.proc;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.extension.util.ScriptHelper;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class LocalCommandControlImpl extends CommandControlImpl {

    private Process process;

    public LocalCommandControlImpl(
            ShellProcessControl parent,
            @NonNull FailableFunction<ShellProcessControl, String, Exception> command,
            FailableFunction<ShellProcessControl, String, Exception> terminalCommand) {
        super(parent, command, terminalCommand);
    }

    @Override
    public boolean waitFor() {
        try {
            if (exitTimeout != null && process.waitFor(exitTimeout, TimeUnit.MILLISECONDS)) {
                exitCode = process.exitValue();
                return true;
            }

            if (exitTimeout == null) {
                exitCode = process.waitFor();
                return true;
            }

            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }

    @Override
    public String prepareTerminalOpen() throws Exception {
        try (var ignored = parent.start()) {
            var operator = parent.getShellType().getConcatenationOperator();
            var consoleCommand = terminalCommand.apply(parent)
                    + operator
                    + parent.getShellType().getPauseCommand();
            return parent.prepareIntermediateTerminalOpen(consoleCommand);
        }
    }

    @Override
    public void closeStdin() throws IOException {
        process.getOutputStream().close();
    }

    @Override
    public boolean isStdinClosed() {
        return false;
    }

    @Override
    public void close() throws IOException {
        waitFor();
    }

    @Override
    public void kill() throws Exception {
        process.destroyForcibly();
    }

    @Override
    public CommandProcessControl start() throws Exception {
        var cmd = command.apply(parent);
        var file = cmd.contains("\n") ? ScriptHelper.createLocalExecScript(cmd) : cmd;
        process = new ProcessBuilder(parent.getShellType().executeCommandListWithShell(file)).start();
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
}
