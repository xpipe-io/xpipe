package io.xpipe.core.store;

import io.xpipe.core.util.SupportedOs;

import java.io.InputStream;
import java.io.OutputStream;

public interface MachineStore extends FileSystemStore, ShellStore {

    @Override
    default void validate() throws Exception {
        try (ShellProcessControl pc = create().start()) {
        }
    }

    public default boolean isLocal() {
        return false;
    }

    public default String queryMachineName() throws Exception {
        try (var pc = create().start()) {
            var operatingSystem = SupportedOs.determine(pc);
            return operatingSystem.determineOperatingSystemName(pc);
        }
    }

    @Override
    public default InputStream openInput(String file) throws Exception {
        return create().commandListFunction(proc -> proc.getShellType().createFileReadCommand(file))
                .startExternalStdout();
    }

    @Override
    public default OutputStream openOutput(String file) throws Exception {
        return create().commandListFunction(proc -> proc.getShellType().createFileWriteCommand(file))
                .startExternalStdin();
    }

    @Override
    public default boolean exists(String file) throws Exception {
        var r = create().commandListFunction(proc -> proc.getShellType().createFileExistsCommand(file))
                .start()
                .discardAndCheckExit();
        return r;
    }

    @Override
    public default boolean mkdirs(String file) throws Exception {
        var r = create().commandListFunction(proc -> proc.getShellType().createMkdirsCommand(file))
                .start()
                .discardAndCheckExit();
        return r;
    }
}
