package io.xpipe.core.store;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;

public interface ShellStore extends DataStore {

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

    ShellProcessControl create();

    public default ShellType determineType() throws Exception {
        try (var pc = create().start()) {
            return pc.getShellType();
        }
    }

    @Override
    default void validate() throws Exception {
        try (ShellProcessControl pc = create().start()) {
        }
    }

    public default String queryMachineName() throws Exception {
        try (var pc = create().start()) {
            var operatingSystem = pc.getOsType();
            return operatingSystem.determineOperatingSystemName(pc);
        }
    }
}
