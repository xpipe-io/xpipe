package io.xpipe.app.storage;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.prefs.AppPrefs;

public class DataStorageMigratedDialog {

    public static void showIfNeeded() {
        var cached = AppCache.getBoolean("vaultMigrated", false);
        if (!cached) {
            return;
        }

        var hadAuth = AppCache.getBoolean("vaultMigratedAuth", false);
        if (!hadAuth) {
            return;
        }


        var gitSync = AppPrefs.get().storageGitRemote().getValue() != null;
        var modal = ModalOverlay.of("vaultMigratedTitle", AppDialog.dialogTextKey(gitSync ? "vaultMigratedGitContent" : "vaultMigratedContent"));
        modal.addButton(new ModalButton("openSettings", () -> AppPrefs.get().selectCategory("vaultAccess"), true, true));
        modal.show();

        AppCache.clear("vaultMigrated");
        AppCache.clear("vaultMigratedAuth");
    }
}
