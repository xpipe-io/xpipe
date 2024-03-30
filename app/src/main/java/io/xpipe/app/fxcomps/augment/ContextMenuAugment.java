package io.xpipe.app.fxcomps.augment;

import io.xpipe.app.fxcomps.CompStructure;
import javafx.geometry.Side;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ContextMenuAugment<S extends CompStructure<?>> implements Augment<S> {

    private final Predicate<MouseEvent> mouseEventCheck;
    private final Predicate<KeyEvent> keyEventCheck;
    private final Supplier<ContextMenu> contextMenu;

    public ContextMenuAugment(Predicate<MouseEvent> mouseEventCheck, Predicate<KeyEvent> keyEventCheck, Supplier<ContextMenu> contextMenu) {
        this.mouseEventCheck = mouseEventCheck;
        this.keyEventCheck = keyEventCheck;
        this.contextMenu = contextMenu;
    }

    @Override
    public void augment(S struc) {
        var currentContextMenu = new AtomicReference<ContextMenu>();

        Supplier<Boolean> hide = () -> {
            if (currentContextMenu.get() != null && currentContextMenu.get().isShowing()) {
                currentContextMenu.get().hide();
                currentContextMenu.set(null);
                return true;
            }
            return false;
        };

        var r = struc.get();
        r.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (mouseEventCheck != null && mouseEventCheck.test(event)) {
                if (!hide.get()) {
                    var cm = contextMenu.get();
                    if (cm != null) {
                        cm.show(r, event.getScreenX(), event.getScreenY());
                        currentContextMenu.set(cm);
                    }
                }

                event.consume();
            }
        });
        r.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (mouseEventCheck != null && mouseEventCheck.test(event)) {
                event.consume();
            }
        });

        r.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (keyEventCheck != null && keyEventCheck.test(event)) {
                event.consume();
            }
        });
        r.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (keyEventCheck != null && keyEventCheck.test(event)) {
                if (!hide.get()) {
                    var cm = contextMenu.get();
                    if (cm != null) {
                        cm.show(r, Side.BOTTOM, 0, 0);
                        currentContextMenu.set(cm);
                    }
                }
                event.consume();
            }
        });

        if (r instanceof ButtonBase buttonBase && keyEventCheck == null) {
            buttonBase.setOnAction(event -> {
                if (!hide.get()) {
                    var cm = contextMenu.get();
                    if (cm != null) {
                        cm.show(r, Side.BOTTOM, 0, 0);
                        currentContextMenu.set(cm);
                    }
                }
                event.consume();
            });
        }
    }
}
