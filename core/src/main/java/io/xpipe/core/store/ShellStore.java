package io.xpipe.core.store;

import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessControl;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;

import java.nio.charset.Charset;

public interface ShellStore extends DataStore, InternalCacheDataStore, LaunchableStore, FileSystemStore, ValidatableStore {

    static boolean isLocal(ShellStore s) {
        return s instanceof LocalStore;
    }

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(control(), this);
    }

    @Override
    default ProcessControl prepareLaunchCommand() throws Exception {
        return control();
    }

    default ShellDialect getShellType() {
        return getCache("type", ShellDialect.class, null);
    }

    default OsType getOsType() {
        return getOrCompute("os", OsType.class, () -> {
            try (var sc = control().start()) {
                return sc.getOsType();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    default Charset getCharset() {
        return getCache("charset", Charset.class, null);
    }

    ShellControl control();

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
