package io.xpipe.core.store;

import io.xpipe.core.util.SecretValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface ShellStore extends DataStore {

    public default Integer getTimeout() {
        return null;
    }

    public default List<SecretValue> getInput() {
        return List.of();
    }

    public default String executeAndRead(List<String> cmd, Integer timeout) throws Exception {
        var pc = prepareCommand(List.of(), cmd, getEffectiveTimeOut(timeout));
        pc.start();
        pc.discardErr();
        var string = new String(pc.getStdout().readAllBytes(), pc.getCharset());
        return string;
    }

    public default String executeAndCheckOut(List<SecretValue> in, List<String> cmd, Integer timeout)
            throws ProcessOutputException, Exception {
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

    public default Optional<String> executeAndCheckErr(List<SecretValue> in, List<String> cmd) throws Exception {
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

    public default Integer getEffectiveTimeOut(Integer timeout) {
        if (this.getTimeout() == null) {
            return timeout;
        }
        if (timeout == null) {
            return getTimeout();
        }
        return Math.min(getTimeout(), timeout);
    }

    public default ProcessControl prepareCommand(List<String> cmd, Integer timeout) throws Exception {
        return prepareCommand(List.of(), cmd, timeout);
    }

    public abstract ProcessControl prepareCommand(List<SecretValue> input, List<String> cmd, Integer timeout)
            throws Exception;

    public default ProcessControl preparePrivilegedCommand(List<String> cmd, Integer timeout) throws Exception {
        return preparePrivilegedCommand(List.of(), cmd, timeout);
    }

    public default ProcessControl preparePrivilegedCommand(List<SecretValue> input, List<String> cmd, Integer timeout)
            throws Exception {
        throw new UnsupportedOperationException();
    }
}
