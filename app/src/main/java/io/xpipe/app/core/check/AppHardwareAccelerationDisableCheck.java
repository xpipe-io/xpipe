package io.xpipe.app.core.check;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.OsType;

public class AppHardwareAccelerationDisableCheck {

    public static void check() {
        if (OsType.ofLocal() != OsType.LINUX) {
            return;
        }

        var cached = AppCache.getBoolean("hardwareAccelerationDisabled", false);
        if (!cached) {
            return;
        }

        AppCache.clear("hardwareAccelerationDisabled");

        ErrorEventFactory.fromMessage(
                        "A graphics driver issue was detected and the application has been restarted. Hardware acceleration has been disabled.")
                .expected()
                .handle();
    }
}
