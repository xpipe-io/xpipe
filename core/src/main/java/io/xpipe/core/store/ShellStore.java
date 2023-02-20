package io.xpipe.core.store;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellDialect;

import java.nio.charset.Charset;

public interface ShellStore extends DataStore, StatefulDataStore, LaunchableStore, FileSystemStore {

    public static MachineStore local() {
        return new LocalStore();
    }

    static void withLocal(Charsetter.FailableConsumer<ShellProcessControl, Exception> c) throws Exception {
        try (var l = local().create().start()) {
            c.accept(l);
        }
    }

    static boolean isLocal(ShellStore s) {
        return s instanceof LocalStore;
    }

    @Override
    default FileSystem createFileSystem() {
        return new ConnectionFileSystem(create());
    }

    @Override
    default String prepareLaunchCommand() throws Exception {
        return create().prepareTerminalOpen();
    }

    default ShellProcessControl create() {
        var pc = createControl();
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
        return getState("os", OsType.class, null);
    }

    default Charset getCharset() {
        return getState("charset", Charset.class, null);
    }

    ShellProcessControl createControl();

    public default ShellDialect determineType() throws Exception {
        try (var pc = create().start()) {
            return pc.getShellDialect();
        }
    }

    @Override
    default void validate() throws Exception {
        try (ShellProcessControl pc = create().start()) {}
    }

    public default String queryMachineName() throws Exception {
        try (var pc = create().start()) {
            var operatingSystem = pc.getOsType();
            return operatingSystem.determineOperatingSystemName(pc);
        }
    }
}
