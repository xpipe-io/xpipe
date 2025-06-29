package io.xpipe.app.core.check;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;

public class AppGnomeScaleDialog {

    public static void showIfNeeded() {
        var shown = AppCache.getBoolean("gnomeScaleNoticeShown", false);
        if (shown) {
            return;
        }

        if (AppMainWindow.getInstance() == null) {
            return;
        }

        var session = System.getenv("XDG_SESSION_TYPE");
        var wayland = session != null && session.toLowerCase().contains("wayland");
        if (!wayland) {
            return;
        }

        var desktop = System.getenv("XDG_CURRENT_DESKTOP");
        var gnome = desktop != null && desktop.toLowerCase().contains("gnome");
        if (!gnome) {
            return;
        }

        var content = AppDialog.dialogText("You are running XPipe with a high display scaling on a Wayland system."
                + " Due to xwayland limitations, this might result in a blurry window."
                + " See below for a possible fix.");
        var modal = ModalOverlay.of("waylandScalingTitle", content);
        modal.addButton(ModalButton.ok(() -> {
            AppCache.update("gnomeScaleNoticeShown", true);
        }));
        modal.show();
    }
}
