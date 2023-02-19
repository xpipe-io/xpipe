package io.xpipe.core.store;

public interface MachineStore extends FileSystemStore, ShellStore {

    public default boolean isLocal() {
        return false;
    }

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(create());
    }
}
