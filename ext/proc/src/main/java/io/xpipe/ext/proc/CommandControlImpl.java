package io.xpipe.ext.proc;

import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.extension.event.TrackEvent;
import lombok.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public abstract class CommandControlImpl extends ProcessControlImpl implements CommandProcessControl {

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

    protected final ShellProcessControl parent;

    @NonNull
    protected final FailableFunction<ShellProcessControl, String, Exception> command;

    protected final FailableFunction<ShellProcessControl, String, Exception> terminalCommand;
    protected boolean elevated;
    protected int exitCode = -1;
    protected String timedOutError;
    protected Integer exitTimeout;
    protected boolean complex;
    protected boolean obeysReturnValueConvention = true;

    public CommandControlImpl(
            ShellProcessControl parent,
            @NonNull FailableFunction<ShellProcessControl, String, Exception> command,
            FailableFunction<ShellProcessControl, String, Exception> terminalCommand) {
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
    public ShellType getShellType() {
        return parent.getShellType();
    }

    @Override
    public CommandProcessControl complex() {
        this.complex = true;
        return this;
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
        var success = (obeysReturnValueConvention && exitCode == 0)
                || (!obeysReturnValueConvention
                        && !(read.get().isEmpty() && !readError.get().isEmpty()));
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
        var success = (obeysReturnValueConvention && exitCode == 0)
                || (!obeysReturnValueConvention
                        && !(read.get().isEmpty() && !readError.get().isEmpty()));
        if (!success) {
            throw new ProcessOutputException(
                    "Command returned with " + exitCode + ": " + readError.get().trim());
        }
    }
}
