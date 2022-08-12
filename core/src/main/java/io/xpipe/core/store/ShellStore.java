package io.xpipe.core.store;

import io.xpipe.core.util.Secret;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ShellStore implements DataStore {

    public Integer getTimeout() {
        return null;
    }

    public List<Secret> getInput() {
        return List.of();
    }

    public boolean isLocal() {
        return false;
    }

    public String executeAndRead(List<String> cmd, Integer timeout) throws Exception {
        var pc = prepareCommand(List.of(), cmd, getEffectiveTimeOut(timeout));
        pc.start();
        pc.discardErr();
        var string = new String(pc.getStdout().readAllBytes(), pc.getCharset());
        return string;
    }

    public String executeAndCheckOut(List<Secret> in, List<String> cmd, Integer timeout) throws ProcessOutputException, Exception {
        var pc = prepareCommand(in, cmd, getEffectiveTimeOut(timeout));
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

        var read = new ByteArrayOutputStream();
        var t = new Thread(() -> {
            try {
                final byte[] buf = new byte[1];

                int length;
                    while ((length = pc.getStdout().read(buf)) > 0) {
                        read.write(buf, 0, length);
                    }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();

        var ec = pc.waitFor();
        var readOut = read.toString(pc.getCharset());
        if (ec == -1) {
            throw new ProcessOutputException("Command timed out");
        }

        if (ec == 0) {
            return readOut;
        } else {
            throw new ProcessOutputException("Command returned with " + ec + ": " + readError.get());
        }
    }

    public Optional<String> executeAndCheckErr(List<Secret> in, List<String> cmd) throws Exception {
        var pc = prepareCommand(in, cmd, getTimeout());
        pc.start();
        var outT = pc.discardOut();

        AtomicReference<String> read = new AtomicReference<>();
        var t = new Thread(() -> {
            try {
                read.set(new String(pc.getStderr().readAllBytes(), pc.getCharset()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        t.setDaemon(true);
        t.start();

        outT.join();
        t.join();

        var ec = pc.waitFor();
        return ec != 0 ? Optional.of(read.get()) : Optional.empty();
    }

    public Integer getEffectiveTimeOut(Integer timeout) {
        if (this.getTimeout() == null) {
            return timeout;
        }
        if (timeout == null) {
            return getTimeout();
        }
        return Math.min(getTimeout(), timeout);
    }

    public ProcessControl prepareCommand(List<String> cmd, Integer timeout) throws Exception {
        return prepareCommand(List.of(), cmd, timeout);
    }

    public abstract ProcessControl prepareCommand(List<Secret> input, List<String> cmd, Integer timeout) throws Exception;

    public ProcessControl preparePrivilegedCommand(List<String> cmd, Integer timeout) throws Exception {
        return preparePrivilegedCommand(List.of(), cmd, timeout);
    }

    public ProcessControl preparePrivilegedCommand(List<Secret> input, List<String> cmd, Integer timeout) throws Exception {
        throw new UnsupportedOperationException();
    }
}
