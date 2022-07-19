package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface ShellProcessStore extends StandardShellStore {

    ShellType determineType() throws Exception;

    @Override
    default InputStream openInput(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileReadCommand(file);
        var p = prepareCommand(InputStream.nullInputStream(), cmd);
        p.start();
        return p.getStdout();
    }

    @Override
    default OutputStream openOutput(String file) throws Exception {
        return null;
//        var type = determineType();
//        var cmd = type.createFileWriteCommand(file);
//        var p = prepare(cmd).redirectErrorStream(true);
//        var proc = p.start();
//        return proc.getOutputStream();
    }

    @Override
    default boolean exists(String file) throws Exception {
        var type = determineType();
        var cmd = type.createFileExistsCommand(file);
        var p = prepareCommand(InputStream.nullInputStream(), cmd);
        p.start();
        return p.waitFor() == 0;
    }
}
