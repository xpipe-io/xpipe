package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;

public interface StandardShellStore extends ShellStore {

    static interface ShellType {

        List<String> createFileReadCommand(String file);

        List<String> createFileWriteCommand(String file);

        List<String> createFileExistsCommand(String file);

        Charset getCharset();

        String getName();
    }

    default String executeAndRead(List<String> cmd) throws Exception {
        var type = determineType();
        var p = prepare(cmd).redirectErrorStream(true);
        var proc = p.start();
        var s = new String(proc.getInputStream().readAllBytes(), type.getCharset());
        return s;
    }

    List<String> createCommand(List<String> cmd);

    ShellType determineType();

    @Override
    default InputStream openInput(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileReadCommand(file);
        var p = prepare(cmd).redirectErrorStream(true);
        var proc = p.start();
        return proc.getInputStream();
    }

    @Override
    default OutputStream openOutput(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileWriteCommand(file);
        var p = prepare(cmd).redirectErrorStream(true);
        var proc = p.start();
        return proc.getOutputStream();
    }

    @Override
    default boolean exists(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileExistsCommand(file);
        var p = prepare(cmd).redirectErrorStream(true);
        var proc = p.start();
        return proc.waitFor() == 0;
    }
}
