package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.process.ShellControl;

public enum ElevationAccess {

    ALLOW {
        @Override
        public boolean requestElevationUsage(ShellControl shellControl) {
            return true;
        }
    },
    ASK {
        @Override
        public boolean requestElevationUsage(ShellControl shellControl) {
            var name = shellControl.getSourceStore().flatMap(shellStore -> DataStorage.get().getStoreEntryIfPresent(shellStore))
                    .map(entry -> entry.getName()).orElse("a system");
            return AppWindowHelper.showConfirmationAlert(
                    AppI18n.observable("elevationRequestTitle"),
                    AppI18n.observable("elevationRequestHeader", name),
                    AppI18n.observable("elevationRequestDescription")
            );
        }
    },
    DENY {
        @Override
        public boolean requestElevationUsage(ShellControl shellControl) {
            return false;
        }
    };

    public boolean requestElevationUsage(ShellControl shellControl) {
        return false;
    }

    public static boolean request(ShellControl shellControl) {
        if (AppPrefs.get() == null) {
            return true;
        }

        return AppPrefs.get().elevationPolicy().getValue().requestElevationUsage(shellControl);
    }
}
