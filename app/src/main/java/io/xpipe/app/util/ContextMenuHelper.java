package io.xpipe.app.util;

import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Region;

public class ContextMenuHelper {

    public static ContextMenu create() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);
        InputHelper.onLeft(contextMenu, false, e -> {
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
                    first.getStyleableNode().requestFocus();
                }
            });
        });
        return contextMenu;
    }

    public static MenuItem item(LabelGraphic graphic, String name) {
        var i = new MenuItem(name, graphic.createGraphicNode());
        return i;
    }

    public static void toggleShow(ContextMenu contextMenu, Node ref, Side side) {
        if (!contextMenu.isShowing()) {
            // Prevent NPE in show()
            if (contextMenu.getScene() == null || ref == null || ref.getScene() == null) {
                return;
            }
            contextMenu.show(ref, side, 0, 0);
        } else {
            contextMenu.hide();
        }
    }
}
