package io.xpipe.ext.proc;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.ext.proc.util.ElevationHelper;
import io.xpipe.ext.proc.util.ShellHelper;
import io.xpipe.ext.proc.util.ShellReader;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.ScriptHelper;
import io.xpipe.extension.util.ThreadHelper;
import lombok.NonNull;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public class CommandProcessControlImpl extends ProcessControlImpl implements CommandProcessControl {

    private static final ExecutorService stdoutReader = Executors.newFixedThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("stdout reader");
            return t;
        }
    });
    private static final ExecutorService stderrReader = Executors.newFixedThreadPool(1, new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            t.setName("stderr reader");
            return t;
        }
    });

    private final ShellProcessControl parent;
    @NonNull
    private final Function<ShellProcessControl, String> command;
    private final Function<ShellProcessControl, String> terminalCommand;
    private CommandProcessControlInputStream stdout;
    private CommandProcessControlInputStream stderr;
    private boolean elevated;
    private boolean manageParent;
    private int exitCode = -1;
    private boolean oneStreamFinished;
    private String timedOutError;
    private Integer exitTimeout;
    private boolean complex;
    private boolean obeysReturnValueConvention = true;

    public CommandProcessControlImpl(
            ShellProcessControl parent,
            @NonNull Function<ShellProcessControl, String> command,
            Function<ShellProcessControl, String> terminalCommand) {
        this.command = command;
        this.parent = parent;
        this.terminalCommand = terminalCommand;
    }

    @Override
    public CommandProcessControl doesNotObeyReturnValueConvention() {
        this.obeysReturnValueConvention = false;
        return this;
    }

    @Override
    public CommandProcessControl sensitive() {
        this.sensitive = true;
        return this;
    }

    @Override
    public String prepareTerminalOpen() throws Exception {
        try (var ignored = parent.start()) {
            var operator = parent.getShellType().getConcatenationOperator();
            var consoleCommand = (elevated
                            ? ElevationHelper.elevateTerminalCommand(terminalCommand.apply(parent), parent)
                            : terminalCommand.apply(parent))
                    + operator
                    + parent.getShellType().getPauseCommand();
            return parent.prepareTerminalOpen(consoleCommand);
        }
    }

    public final void closeStdin() throws IOException {
        parent.closeStdin();
    }

    @Override
    public boolean isStdinClosed() {
        return parent.isStdinClosed();
    }

    public int getExitCode() {
        if (running) {
            waitFor();
        }

        return exitCode;
    }

    @Override
    public CommandProcessControl exitTimeout(Integer timeout) {
        this.exitTimeout = timeout;
        return this;
    }

    @Override
    public synchronized void kill() throws Exception {
        if (!running) {
            return;
        }

        TrackEvent.trace("proc", "Killing command ...");
        parent.kill();
        running = false;
    }

    @Override
    public CommandProcessControl customCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public CommandProcessControl elevated() {
        this.elevated = true;
        return this;
    }

    @Override
    public CommandProcessControlImpl start() throws Exception {
        if (running) {
            return this;
        }

        manageParent = !parent.isRunning();
        parent.start();

        var baseCommand = command.apply(parent);
        this.complex = complex || baseCommand.contains("\n");
        if (complex) {
            var script = ScriptHelper.createExecScript(parent, baseCommand, true);
            baseCommand = "\"" + script + "\"";
        }

        var startStdoutId = UUID.randomUUID();
        var endStdoutId = UUID.randomUUID();
        var startStderrId = UUID.randomUUID();
        var endStderrId = UUID.randomUUID();
        var string = String.format(
                "%s%s %s%s %s%s %s%s %s",
                parent.getShellType().getEchoCommand(startStdoutId.toString(), false),
                parent.getShellType().getConcatenationOperator(),
                parent.getShellType().getEchoCommand(startStderrId.toString(), true),
                parent.getShellType().getConcatenationOperator(),
                baseCommand,
                parent.getShellType().getConcatenationOperator(),
                parent.getShellType()
                        .getPrintVariableCommand(
                                endStdoutId.toString(), parent.getShellType().getExitCodeVariable()),
                parent.getShellType().getConcatenationOperator(),
                parent.getShellType().getEchoCommand(endStderrId.toString(), true));

        TrackEvent.withTrace("proc", "Starting command execution ...")
                .tag("baseCommand", ShellHelper.censor(baseCommand, sensitive))
                .tag("shellType", parent.getShellType().getName())
                .handle();

        if (elevated) {
            string = ElevationHelper.elevateNormalCommand(string, parent, baseCommand);
        }
        parent.executeCommand(string);
        running = true;

        // Check for custom charset
        if (charset == null) {
            charset = parent.getCharset();
        }

        stdout = new CommandProcessControlInputStream(
                parent.getStdout(),
                (startStdoutId.toString() + parent.getShellType().getNewLine().getNewLineString())
                        .getBytes(parent.getCharset()),
                endStdoutId.toString().getBytes(parent.getCharset()),
                this::onStdoutFinish);
        stderr = new CommandProcessControlInputStream(
                parent.getStderr(),
                (startStderrId.toString() + parent.getShellType().getNewLine().getNewLineString())
                        .getBytes(parent.getCharset()),
                (endStderrId.toString() + parent.getShellType().getNewLine().getNewLineString())
                        .getBytes(parent.getCharset()),
                this::onStderrFinish);

        return this;
    }

    @SneakyThrows
    public synchronized void onStdoutFinish(CommandProcessControlInputStream.FinishReason r) {
        if (r == CommandProcessControlInputStream.FinishReason.NORMAL_FINISH) {
            TrackEvent.trace("proc", "Stdout finished. Reading exit code ...");
            var exitCode = ShellReader.readLine(parent.getStdout(), parent.getCharset());
            try {
                this.exitCode = Integer.parseInt(exitCode);
            } catch (NumberFormatException ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        }

        if (r == CommandProcessControlInputStream.FinishReason.START_TIMEOUT) {
            if (getStdout().getPreStartContent().length > 0) {
                timedOutError = new String(getStdout().getPreStartContent(), parent.getCharset());
            }
        }

        TrackEvent.withTrace("proc", "Stdout finished")
                .tag("finishReason", r)
                .tag("exitCode", exitCode)
                .handle();

        updateFinishState(r);
    }

    @SneakyThrows
    public synchronized void onStderrFinish(CommandProcessControlInputStream.FinishReason r) {
        if (r == CommandProcessControlInputStream.FinishReason.START_TIMEOUT) {
            if (getStderr().getPreStartContent().length > 0) {
                timedOutError = new String(getStderr().getPreStartContent(), parent.getCharset());
            }
        }

        TrackEvent.withTrace("proc", "Stderr finished").tag("finishReason", r).handle();

        updateFinishState(r);
    }

    @SneakyThrows
    private synchronized void updateFinishState(CommandProcessControlInputStream.FinishReason r) {
        if (!oneStreamFinished) {
            oneStreamFinished = true;
            return;
        }

        if (r == CommandProcessControlInputStream.FinishReason.START_TIMEOUT) {
            kill();
            return;
        } else {
            TrackEvent.trace("proc", "Command finished");
            running = false;
        }
    }

    @Override
    public ShellType getShellType() {
        return parent.getShellType();
    }

    @Override
    @SneakyThrows
    public void close() throws IOException {
        if (!waitFor()) {
            kill();
            throw new IOException("Command timed out" + (timedOutError != null ? ": " + timedOutError : ""));
        }

        TrackEvent.withTrace("proc", "Closing command")
                .tag("manageParent", manageParent)
                .handle();
        if (manageParent) {
            parent.close();
        }
    }

    @Override
    public CommandProcessControl complex() {
        this.complex = true;
        return this;
    }

    @Override
    public boolean waitFor() {
        if (!running) {
            return exitCode != -1;
        }

        if (exitTimeout == null) {
            exitTimeout = Integer.MAX_VALUE / 10;
        }

        for (int i = 0; i < exitTimeout * 10; i++) {
            if (!running) {
                TrackEvent.withTrace("proc", "Command finished on time")
                        .tag("elapsedTime", i * 100)
                        .handle();
                return exitCode != -1;
            }

            ThreadHelper.sleep(100);
        }

        TrackEvent.withTrace("proc", "Command timed out").handle();
        return false;
    }

    @Override
    public CommandProcessControlInputStream getStdout() {
        return stdout;
    }

    public OutputStream getStdin() {
        return parent.getStdin();
    }

    @Override
    public CommandProcessControlInputStream getStderr() {
        return stderr;
    }

    public void discardOut() {
        stdoutReader.submit(() -> {
            try {
                var read = new String(getStdout().readAllBytes(), getCharset());
                TrackEvent.withTrace("proc", "Discarding stdout")
                        .tag("output", read)
                        .handle();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void discardErr() {
        stderrReader.submit(() -> {
            try {
                var read = new String(getStderr().readAllBytes(), getCharset());
                TrackEvent.withTrace("proc", "Discarding stderr")
                        .tag("output", read)
                        .handle();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String readOnlyStdout() throws Exception {
        discardErr();
        var bytes = getStdout().readAllBytes();
        var string = new String(bytes, getCharset());
        TrackEvent.withTrace("proc", "Read stdout").tag("output", string).handle();
        return string.trim();
    }

    @Override
    public void accumulateStdout(Consumer<String> con) {
        stderrReader.submit(() -> {
            try {
                var out = new String(getStdout().readAllBytes(), getCharset()).strip();
                TrackEvent.withTrace("proc", "Read stdout").tag("output", out).handle();
                con.accept(out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public void accumulateStderr(Consumer<String> con) {
        stderrReader.submit(() -> {
            try {
                var err = new String(getStderr().readAllBytes(), getCharset()).strip();
                TrackEvent.withTrace("proc", "Read stderr").tag("output", err).handle();
                con.accept(err);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public String readOrThrow() throws Exception {
        AtomicReference<String> read = new AtomicReference<>("");
        stdoutReader.submit(() -> {
            try {
                var bytes = getStdout().readAllBytes();
                read.set(new String(bytes, getCharset()));
                TrackEvent.withTrace("proc", "Read stdout")
                        .tag("output", read.get())
                        .handle();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        AtomicReference<String> readError = new AtomicReference<>("");
        stderrReader.submit(() -> {
            try {
                readError.set(new String(getStderr().readAllBytes(), getCharset()));
                TrackEvent.withTrace("proc", "Read stderr")
                        .tag("output", readError.get())
                        .handle();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        var ec = waitFor();
        if (!ec) {
            throw new ProcessOutputException("Command timed out" + (timedOutError != null ? ": " + timedOutError : ""));
        }

        var exitCode = getExitCode();
        var success = (obeysReturnValueConvention && exitCode == 0) || (!obeysReturnValueConvention && !(read.get().isEmpty() && !readError.get().isEmpty()));
        if (success) {
            return read.get().trim();
        } else {
            throw new ProcessOutputException(
                    "Command returned with " + exitCode + ": " + readError.get().trim());
        }
    }

    public void discardOrThrow() throws Exception {
        AtomicReference<String> read = new AtomicReference<>("");
        stdoutReader.submit(() -> {
            try {
                getStdout().transferTo(OutputStream.nullOutputStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        AtomicReference<String> readError = new AtomicReference<>("");
        stderrReader.submit(() -> {
            try {
                readError.set(new String(getStderr().readAllBytes(), getCharset()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        var ec = waitFor();
        if (!ec) {
            throw new ProcessOutputException("Command timed out" + (timedOutError != null ? ": " + timedOutError : ""));
        }

        var exitCode = getExitCode();
        var success = (obeysReturnValueConvention && exitCode == 0) || (!obeysReturnValueConvention && !(read.get().isEmpty() && !readError.get().isEmpty()));
        if (!success) {
            throw new ProcessOutputException(
                    "Command returned with " + exitCode + ": " + readError.get().trim());
        }
    }
}
