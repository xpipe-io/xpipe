package io.xpipe.app.core.check;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;

public class AppGnomeScaleDialog {

    public static void showIfNeeded() {
        var shown = AppCache.getBoolean("gnomeScaleNoticeShown", false);
        if (shown) {
            return;
        }

        if (AppMainWindow.getInstance() == null) {
            return;
        }

        // Only happens for externally set display scale
        if (AppPrefs.get().uiScale().getValue() != null) {
            return;
        }

        var scale = AppMainWindow.getInstance().displayScale().getValue();
        var highDpi = scale.doubleValue() > 1.5;
        TrackEvent.debug("Scale value: " + scale.doubleValue());
        if (!highDpi) {
            // return;
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
