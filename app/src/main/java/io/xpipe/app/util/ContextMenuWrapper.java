package io.xpipe.app.util;

import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.platform.InputHelper;
import io.xpipe.app.prefs.AppPrefs;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ContextMenuWrapper {

    private static final Set<ContextMenu> allContextMenus = new HashSet<>();

    private final Supplier<ContextMenu> contextMenuSupplier;
    private boolean customKeyHandling;

    @Getter
    private ContextMenu contextMenu;

    public ContextMenuWrapper(Supplier<ContextMenu> contextMenuSupplier) {this.contextMenuSupplier = contextMenuSupplier;}

    public ContextMenuWrapper withCustomKeyHandling() {
        customKeyHandling = true;
        return this;
    }

    private void applyFixes() {
        contextMenu.setAutoHide(!AppPrefs.get().limitedTouchscreenMode().get());
        InputHelper.onLeft(contextMenu, true, e -> {
            contextMenu.hide();
            e.consume();
        });
        contextMenu.addEventFilter(Menu.ON_SHOWING, e -> {
            Node content = contextMenu.getSkin().getNode();
            if (content instanceof Region r) {
                r.setMaxWidth(500);
            }
        });
        contextMenu.addEventFilter(Menu.ON_SHOWN, e -> {
            Platform.runLater(() -> {
                var first = contextMenu.getItems().getFirst();
                if (first != null) {
                    var s = first.getStyleableNode();
                    if (s != null) {
                        s.requestFocus();
                    }
                }
            });
        });
        AppFontSizes.lg(contextMenu.getStyleableNode());

        if (!customKeyHandling) {
            contextMenu.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                for (MenuItem item : contextMenu.getItems()) {
                    if (item.getAccelerator() != null && item.getAccelerator().match(e)) {
                        contextMenu.hide();
                        item.fire();
                        e.consume();
                    }
                }
            });
        }
    }

    private ContextMenu getOrCreate() {
        if (contextMenu == null) {
            contextMenu = contextMenuSupplier.get();
            if (contextMenu != null) {
                applyFixes();
            }
        }
        return contextMenu;
    }

    public boolean isHidden() {
        return contextMenu == null || !contextMenu.isShowing();
    }

    public boolean isShowing() {
        return contextMenu != null && contextMenu.isShowing();
    }

    public static void hideAll() {
        for (ContextMenu cm : allContextMenus) {
            cm.hide();
        }
    }

    private void hideOthers(boolean includeSelf) {
        for (ContextMenu cm : allContextMenus) {
            if (!includeSelf && cm.equals(contextMenu)) {
                continue;
            }

            cm.hide();
        }
    }

    public void hide() {
        if (isShowing()) {
            contextMenu.hide();
        }
    }

    public boolean show(Region r, Side side) {
        if (isShowing()) {
            return true;
        }

        var cm = getOrCreate();
        if (cm != null) {
            // Prevent NPE in show()
            if (contextMenu.getScene() == null || r == null || r.getScene() == null) {
                return false;
            }

            cm.show(r, side, 0, 0);

            allContextMenus.add(cm);
            return true;
        } else {
            return false;
        }
    }

    public boolean show(Region r, double x, double y) {
        if (isShowing()) {
            return true;
        }

        var cm = getOrCreate();
        if (cm != null) {
            // Prevent NPE in show()
            if (contextMenu.getScene() == null || r == null || r.getScene() == null) {
                return false;
            }

            cm.show(r, x, y);
            allContextMenus.add(cm);
            return true;
        } else {
            return false;
        }
    }

    public void installOnButton(ButtonBase r) {
        installOnButton(r, Side.BOTTOM);
    }

    public void installOnButton(ButtonBase r, Side side) {
        r.addEventFilter(ActionEvent.ACTION, event -> {
            if (r.getOnAction() != null) {
                return;
            }

            var showing = isShowing();
            hideOthers(showing);
            if (!showing && show(r, side)) {
                event.consume();
            }
        });
    }

    public void installOnMouseClick(Region r, Predicate<MouseEvent> mouseEventCheck, boolean showAtLocation) {
        r.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (mouseEventCheck != null && mouseEventCheck.test(event)) {
                hideOthers(showAtLocation);
                var shown = (showAtLocation && show(r, event.getScreenX(), event.getScreenY())) || (!showAtLocation && show(r, Side.BOTTOM));
                if (shown) {
                    event.consume();
                }
            }
        });
        r.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (mouseEventCheck != null && mouseEventCheck.test(event)) {
                event.consume();
            }
        });

    }

    public void installOnKeyPress(Region r, Predicate<KeyEvent> keyEventCheck) {
        r.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (keyEventCheck != null && keyEventCheck.test(event)) {
                event.consume();
            }
        });
        r.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (keyEventCheck != null) {
                if (keyEventCheck.test(event)) {
                    var showing = isShowing();
                    hideOthers(showing);
                    if (!showing && show(r, Side.BOTTOM)) {
                        event.consume();
                    }
                }
            }
        });
    }
}
