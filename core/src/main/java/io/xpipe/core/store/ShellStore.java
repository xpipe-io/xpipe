package io.xpipe.core.store;

import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.ShellControl;

public interface ShellStore extends DataStore, LaunchableStore, FileSystemStore, ValidatableStore {

    static boolean isLocal(ShellStore s) {
        return s instanceof LocalStore;
    }

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(control());
    }

    default ProcessControl prepareLaunchCommand() {
        return control();
    }

    ShellControl control();

    @Override
    default void validate() throws Exception {
        var c = control();
        if (!isInStorage()) {
            c.withoutLicenseCheck();
        }

        try (ShellControl pc = c.start()) {}
    }
}
