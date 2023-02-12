package io.xpipe.app.util;

import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.util.ThreadHelper;

public class MacOsPermissions {

    public static boolean waitForAccessibilityPermissions() throws Exception {
        try (var pc = ShellStore.local().create().start()) {
            while (true) {
                var success = pc.executeBooleanSimpleCommand("osascript -e 'tell application \"System Events\" to keystroke \"t\"'");
                if (success) {
                    return true;
                }

                ThreadHelper.sleep(1000);
            }
        }
    }
}
