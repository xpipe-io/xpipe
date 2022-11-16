package io.xpipe.core.store;

import java.util.concurrent.atomic.AtomicReference;

public interface ShellStore extends DataStore {

    public static MachineStore local() {
        return new LocalStore();
    }

    ShellProcessControl create();

    public default ShellType determineType() throws Exception {
        AtomicReference<ShellType> type = new AtomicReference<>();
        try (var pc = create().start()) {
            type.set(pc.getShellType());
        }
        return type.get();
    }
}
