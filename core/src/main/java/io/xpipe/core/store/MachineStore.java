package io.xpipe.core.store;

import java.io.InputStream;
import java.io.OutputStream;

public interface MachineStore extends FileSystemStore, ShellStore {

    public default boolean isLocal() {
        return false;
    }

    public default String queryMachineName() throws Exception {
        try (CommandProcessControl pc = create().commandListFunction(shellProcessControl ->
                        shellProcessControl.getShellType().getOperatingSystemNameCommand())
                .start()) {
            return pc.readOrThrow().trim();
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
