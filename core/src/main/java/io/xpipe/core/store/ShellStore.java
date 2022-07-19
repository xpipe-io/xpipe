package io.xpipe.core.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public interface ShellStore extends MachineFileStore {

    static StandardShellStore local() {
        return new LocalStore();
    }

    default String executeAndRead(List<String> cmd) throws Exception {
        var pc = prepareCommand(InputStream.nullInputStream(), cmd);
        pc.start();
        pc.discardErr();
        var string = new String(pc.getStdout().readAllBytes(), pc.getCharset());
        return string;
    }

    default Optional<String> executeAndCheckOut(InputStream in, List<String> cmd) throws Exception {
        var pc = prepareCommand(in, cmd);
        pc.start();
        var outT = pc.discardErr();

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

        outT.join();
        t.join();

        var ec = pc.waitFor();
        return ec == 0 ? Optional.of(read.get()) : Optional.empty();
    }

    default Optional<String> executeAndCheckErr(InputStream in, List<String> cmd) throws Exception {
        var pc = prepareCommand(in, cmd);
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

    default ProcessControl prepareCommand(List<String> cmd) throws Exception {
        return prepareCommand(InputStream.nullInputStream(), cmd);
    }

    ProcessControl prepareCommand(InputStream input, List<String> cmd) throws Exception;

    default ProcessControl preparePrivilegedCommand(List<String> cmd) throws Exception {
        return preparePrivilegedCommand(InputStream.nullInputStream(), cmd);
    }

    default ProcessControl preparePrivilegedCommand(InputStream input, List<String> cmd) throws Exception {
        throw new UnsupportedOperationException();
    }
}
