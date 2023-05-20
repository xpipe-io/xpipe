package io.xpipe.app.fxcomps.augment;

import io.xpipe.app.fxcomps.CompStructure;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;

import java.util.function.Supplier;

public class ContextMenuAugment<S extends CompStructure<?>> implements Augment<S> {

    private final boolean showOnPrimaryButton;
    private final Supplier<ContextMenu> contextMenu;

    public ContextMenuAugment(boolean showOnPrimaryButton, Supplier<ContextMenu> contextMenu) {
        this.showOnPrimaryButton = showOnPrimaryButton;
        this.contextMenu = contextMenu;
    }

    private static ContextMenu currentContextMenu;

    @Override
    public void augment(S struc) {
        var r = struc.get();
        r.setOnMousePressed(event -> {
            if (currentContextMenu != null && currentContextMenu.isShowing()) {
                currentContextMenu.hide();
                currentContextMenu = null;
            }

            if ((showOnPrimaryButton && event.getButton() == MouseButton.PRIMARY)
                    || (!showOnPrimaryButton && event.getButton() == MouseButton.SECONDARY)) {
                var cm = contextMenu.get();
                if (cm != null) {
                    cm.setAutoHide(true);
                    cm.show(r, event.getScreenX(), event.getScreenY());
                    currentContextMenu = cm;
                }

                event.consume();
            }
        });
    }
}
