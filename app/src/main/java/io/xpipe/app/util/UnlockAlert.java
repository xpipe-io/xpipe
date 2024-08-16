package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;

public class UnlockAlert {

    public static void showIfNeeded() {
        if (AppPrefs.get().getLockCrypt().getValue() == null
                || AppPrefs.get().getLockCrypt().getValue().isEmpty()) {
            return;
        }

        if (AppPrefs.get().getLockPassword().getValue() != null) {
            return;
        }

        while (true) {
            var r = AskpassAlert.queryRaw(AppI18n.get("unlockAlertHeader"), null);
            if (r.getState() == SecretQueryState.CANCELLED) {
                ErrorEvent.fromMessage("Unlock cancelled")
                        .expected()
                        .term()
                        .omit()
                        .handle();
                return;
            }

            if (AppPrefs.get().unlock(r.getSecret().inPlace())) {
                return;
            }
        }
    }
}
