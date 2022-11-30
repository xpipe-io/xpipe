package io.xpipe.extension.fxcomps.augment;

import io.xpipe.extension.fxcomps.CompStructure;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;

public abstract class PopupMenuAugment<S extends CompStructure<?>> implements Augment<S> {

    private final boolean showOnPrimaryButton;

    protected PopupMenuAugment(boolean showOnPrimaryButton) {
        this.showOnPrimaryButton = showOnPrimaryButton;
    }

    protected abstract ContextMenu createContextMenu();

    @Override
    public void augment(S struc) {
        var cm = createContextMenu();
        var r = struc.get();
        r.setOnMousePressed(event -> {
            if ((showOnPrimaryButton && event.getButton() == MouseButton.PRIMARY)
                    || (!showOnPrimaryButton && event.getButton() == MouseButton.SECONDARY)) {
                cm.show(r, event.getScreenX(), event.getScreenY());
                event.consume();
            } else {
                cm.hide();
            }
        });
    }
}
