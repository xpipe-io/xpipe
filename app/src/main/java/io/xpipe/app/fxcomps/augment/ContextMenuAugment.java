package io.xpipe.app.fxcomps.augment;

import io.xpipe.app.fxcomps.CompStructure;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class ContextMenuAugment<S extends CompStructure<?>> implements Augment<S> {

    private final Predicate<MouseEvent> show;
    private final Supplier<ContextMenu> contextMenu;

    private static ContextMenu currentContextMenu;

    public ContextMenuAugment(Predicate<MouseEvent> show, Supplier<ContextMenu> contextMenu) {
        this.show = show;
        this.contextMenu = contextMenu;
    }

    public ContextMenuAugment(Supplier<ContextMenu> contextMenu) {
        this.show = event -> event.getButton() == MouseButton.SECONDARY;
        this.contextMenu = contextMenu;
    }

    @Override
    public void augment(S struc) {
        var r = struc.get();
        r.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (currentContextMenu != null && currentContextMenu.isShowing()) {
                currentContextMenu.hide();
                currentContextMenu = null;
            }

            if (show.test(event)) {
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
