package io.xpipe.app.util;

import io.xpipe.core.store.ShellStore;

public class MacOsPermissions {

    public static boolean waitFor() throws Exception {
        try (var pc = ShellStore.local().create().start()) {
            pc.executeSimpleCommand("tell application \"System Events\" to keystroke \"t\"");
        }
        return true;
    }
}
