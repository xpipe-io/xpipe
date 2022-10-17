package io.xpipe.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ProcessControl {

    public String executeAndReadStdout() throws Exception {
        var pc = this;
        pc.start();
        pc.discardErr();
        var bytes = pc.getStdout().readAllBytes();
        var string = new String(bytes, pc.getCharset());
        return string;
    }

    public void executeOrThrow() throws Exception {
        var pc = this;
        pc.start();
        pc.discardOut();
        pc.discardErr();
        pc.waitFor();
    }

    public Optional<String> executeAndReadStderrIfPresent() throws Exception {
        var pc = this;
        pc.start();
        pc.discardOut();
        var bytes = pc.getStderr().readAllBytes();
        var string = new String(bytes, pc.getCharset());
        var ec = pc.waitFor();
        return ec != 0 ? Optional.of(string) : Optional.empty();
    }

    public String executeAndReadStdoutOrThrow()
            throws Exception {
        var pc = this;
        pc.start();

        AtomicReference<String> readError = new AtomicReference<>();
        var errorThread = new Thread(() -> {
            try {

                readError.set(new String(pc.getStderr().readAllBytes(), pc.getCharset()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        errorThread.setDaemon(true);
        errorThread.start();

        AtomicReference<String> read = new AtomicReference<>();
        var t = new Thread(() -> {
            try {
                read.set(new String(pc.getStdout().readAllBytes(), pc.getCharset()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();

        var ec = pc.waitFor();
        if (ec == -1) {
            throw new ProcessOutputException("Command timed out");
        }

        if (ec == 0 && !(read.get().isEmpty() && !readError.get().isEmpty())) {
            return read.get().trim();
        } else {
            throw new ProcessOutputException(
                    "Command returned with " + ec + ": " + readError.get().trim());
        }
    }

    public Thread discardOut() {
        var t = new Thread(() -> {
            try {
                getStdout().transferTo(OutputStream.nullOutputStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    public Thread discardErr() {
        var t = new Thread(() -> {
            try {
                getStderr().transferTo(OutputStream.nullOutputStream());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }

    public abstract void start() throws Exception;

    public abstract int waitFor() throws Exception;

    public abstract InputStream getStdout();

    public abstract OutputStream getStdin();

    public abstract InputStream getStderr();

    public abstract Charset getCharset();
}
