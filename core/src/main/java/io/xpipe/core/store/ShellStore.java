package io.xpipe.core.store;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;

import java.nio.charset.Charset;

public interface ShellStore extends DataStore, InternalCacheDataStore, LaunchableStore, FileSystemStore {

    static ShellStore createLocal() {
        return new LocalStore();
    }

    static boolean isLocal(ShellStore s) {
        return s instanceof LocalStore;
    }

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(control(), this);
    }

    @Override
    default String prepareLaunchCommand(String displayName) throws Exception {
        return control().prepareTerminalOpen(displayName);
    }

    default ShellControl control() {
        var pc = createBasicControl();
        pc.onInit(processControl -> {
            setState("type", processControl.getShellDialect());
            setState("os", processControl.getOsType());
            setState("charset", processControl.getCharset());
        });
        return pc;
    }

    default ShellDialect getShellType() {
        return getState("type", ShellDialect.class, null);
    }

    default OsType getOsType() {
        return getOrComputeState("os", OsType.class, () -> {
            try (var sc = control().start()) {
                return sc.getOsType();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    default Charset getCharset() {
        return getState("charset", Charset.class, null);
    }

    ShellControl createBasicControl();

    default ShellDialect determineType() throws Exception {
        try (var pc = control().start()) {
            return pc.getShellDialect();
        }
    }

    @Override
    default void validate() throws Exception {
        try (ShellControl pc = control().start()) {}
    }

    default String queryMachineName() throws Exception {
        try (var pc = control().start()) {
            var operatingSystem = pc.getOsType();
            return operatingSystem.determineOperatingSystemName(pc);
        }
    }
}
