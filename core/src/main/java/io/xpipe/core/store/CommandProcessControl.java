package io.xpipe.core.store;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface CommandProcessControl extends ProcessControl {

    default InputStream startExternalStdout() throws Exception {
        start();
        discardErr();
        return new FilterInputStream(getStdout()) {
            @Override
            public void close() throws IOException {
                getStdout().close();
                CommandProcessControl.this.close();
            }
        };
    }

    default OutputStream startExternalStdin() throws Exception {
        try (CommandProcessControl pc = start()) {
            pc.discardOut();
            pc.discardErr();
            return new FilterOutputStream(getStdin()) {
                @Override
                public void close() throws IOException {
                    pc.getStdin().close();
                    pc.close();
                }
            };
        } catch (Exception e) {
            throw e;
        }
    }

    CommandProcessControl customCharset(Charset charset);

    int getExitCode();

    CommandProcessControl elevated();

    @Override
    CommandProcessControl start() throws Exception;

    @Override
    CommandProcessControl exitTimeout(int timeout);

    String readOnlyStdout() throws Exception;

    public default void discardOrThrow() throws Exception {
        readOrThrow();
    }

    public default boolean startAndCheckExit() {
        try (var pc = start()) {
            return pc.discardAndCheckExit();
        } catch (Exception e) {
            return false;
        }
    }

    public default boolean discardAndCheckExit() {
        try {
            discardOrThrow();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public default Optional<String> readStderrIfPresent() throws Exception {
        discardOut();
        var bytes = getStderr().readAllBytes();
        var string = new String(bytes, getCharset());
        var ec = waitFor();
        return ec ? Optional.of(string) : Optional.empty();
    }

    public default String readOrThrow() throws Exception {
        AtomicReference<String> readError = new AtomicReference<>("");
        var errorThread = new Thread(() -> {
            try {

                readError.set(new String(getStderr().readAllBytes(), getCharset()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();

        AtomicReference<String> read = new AtomicReference<>("");
        var t = new Thread(() -> {
            try {
                read.set(readLine());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();

        var ec = waitFor();
        if (!ec) {
            throw new ProcessOutputException("Command timed out");
        }

        var exitCode = getExitCode();
        if (exitCode == 0 && !(read.get().isEmpty() && !readError.get().isEmpty())) {
            return read.get().trim();
        } else {
            throw new ProcessOutputException(
                    "Command returned with " + ec + ": " + readError.get().trim());
        }
    }

    Thread discardOut();

    Thread discardErr();
}
