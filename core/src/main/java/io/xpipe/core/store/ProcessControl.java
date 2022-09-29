package io.xpipe.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ProcessControl {

    public String readOutOnly() throws Exception {
        start();
        var errT = discardErr();
        var string = new String(getStdout().readAllBytes(), getCharset());
        waitFor();
        return string;
    }

    public Optional<String> readErrOnly() throws Exception {
        start();
        var outT = discardOut();

        AtomicReference<String> read = new AtomicReference<>();
        var t = new Thread(() -> {
            try {
                read.set(new String(getStderr().readAllBytes(), getCharset()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();

        var ec = waitFor();
        return ec != 0 ? Optional.of(read.get()) : Optional.empty();
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
