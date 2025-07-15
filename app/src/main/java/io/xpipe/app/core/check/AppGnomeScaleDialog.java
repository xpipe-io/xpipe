package io.xpipe.app.core.check;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.util.DocumentationLink;

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

        var content = AppDialog.dialogText(
                "You are running XPipe on a Wayland system."
                        + " If you are using a high-dpi display, due to xwayland limitations, this might result in a blurry window. See the documentation for workarounds if you are affected.");
        var modal = ModalOverlay.of("waylandScalingTitle", content);
        modal.addButton(new ModalButton(
                "docs",
                () -> {
                    DocumentationLink.GNOME_WAYLAND_SCALING.open();
                },
                false,
                false));
        modal.addButton(ModalButton.ok(() -> {
            AppCache.update("gnomeScaleNoticeShown", true);
        }));
        modal.show();
    }
}
