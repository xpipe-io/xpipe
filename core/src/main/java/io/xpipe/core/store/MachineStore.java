package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface MachineStore extends FileSystemStore, ShellStore {

    public default boolean isLocal() {
        return false;
    }

    @Override
    public default InputStream openInput(String file) throws Exception {
        return create().command(proc -> proc.getShellType()
                        .createFileReadCommand(proc.getOsType().normalizeFileName(file)))
                .startExternalStdout();
    }

    @Override
    public default OutputStream openOutput(String file) throws Exception {
        return create().command(proc -> proc.getShellType()
                        .createFileWriteCommand(proc.getOsType().normalizeFileName(file)))
                .startExternalStdin();
    }

    @Override
    public default boolean exists(String file) throws Exception {
        try (var pc = create().command(proc -> proc.getShellType()
                        .createFileExistsCommand(proc.getOsType().normalizeFileName(file)))
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public default boolean mkdirs(String file) throws Exception {
        try (var pc = create().command(proc -> proc.getShellType()
                        .flatten(proc.getShellType()
                                .createMkdirsCommand(proc.getOsType().normalizeFileName(file))))
                .start()) {
            return pc.discardAndCheckExit();
        }
    }
}
