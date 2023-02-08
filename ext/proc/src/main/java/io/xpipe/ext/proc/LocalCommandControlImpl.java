package io.xpipe.ext.proc;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.extension.util.ScriptHelper;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class LocalCommandControlImpl extends CommandControlImpl {

    private Process process;

    public LocalCommandControlImpl(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> command,
            Function<ShellProcessControl, String> terminalCommand
    ) {
        super(parent, command, terminalCommand);
    }

    @Override
    public boolean waitFor() {
        try {
            return process.waitFor(exitTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
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
        var file = ScriptHelper.createLocalExecScript(command.apply(parent));
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
