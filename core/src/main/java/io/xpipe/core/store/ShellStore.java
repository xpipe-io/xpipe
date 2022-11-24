package io.xpipe.core.store;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellType;

public interface ShellStore extends DataStore {

    public static MachineStore local() {
        return new LocalStore();
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
}
