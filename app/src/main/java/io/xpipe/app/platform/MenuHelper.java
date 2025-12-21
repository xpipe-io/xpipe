package io.xpipe.app.platform;

import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;

import io.xpipe.app.update.AppDistributionType;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.ComboBoxPopupControl;
import javafx.scene.layout.Region;
import lombok.SneakyThrows;

import java.util.function.Function;

public class MenuHelper {

    @SneakyThrows
    public static <T> ComboBoxListViewSkin<T> fixComboBoxSkin(ComboBoxListViewSkin<T> skin) {
        var m = ComboBoxPopupControl.class.getDeclaredMethod("getPopup");
        m.setAccessible(true);
        var popup = (PopupControl) m.invoke(skin);
        popup.setAutoHide(AppDistributionType.get() != AppDistributionType.ANDROID_LINUX_TERMINAL);
        return skin;
    }

    @SneakyThrows
    public static <T> ComboBox<T> createComboBox() {
        var cb = new ComboBox<T>();
        var skin = new ComboBoxListViewSkin<>(cb);
        fixComboBoxSkin(skin);
        cb.setSkin(skin);
        return cb;
    }

    public static ContextMenu createContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(AppDistributionType.get() != AppDistributionType.ANDROID_LINUX_TERMINAL);
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
                    var s = first.getStyleableNode();
                    if (s != null) {
                        s.requestFocus();
                    }
                }
            });
        });
        AppFontSizes.lg(contextMenu.getStyleableNode());
        return contextMenu;
    }

    public static MenuItem createMenuItem(LabelGraphic graphic, String nameKey) {
        var i = new MenuItem();
        i.textProperty().bind(AppI18n.observable(nameKey));
        i.setGraphic(graphic.createGraphicNode());
        return i;
    }

    public static void toggleMenuShow(ContextMenu contextMenu, Node ref, Side side) {
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
