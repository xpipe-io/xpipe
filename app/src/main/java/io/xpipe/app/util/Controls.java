/* SPDX-License-Identifier: MIT */

package io.xpipe.app.util;

import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URI;

import static atlantafx.base.theme.Styles.BUTTON_ICON;

public final class Controls {

    public static Button iconButton(Ikon icon, boolean disable) {
        return button("", icon, disable, BUTTON_ICON);
    }

    public static Button button(String text, Ikon icon, boolean disable, String... styleClasses) {
        var button = new Button(text);
        if (icon != null) {
            button.setGraphic(new FontIcon(icon));
        }
        button.setDisable(disable);
        button.getStyleClass().addAll(styleClasses);
        return button;
    }

    public static MenuItem menuItem(String text, Ikon graphic, KeyCombination accelerator) {
        return menuItem(text, graphic, accelerator, false);
    }

    public static MenuItem menuItem(String text, Ikon graphic, KeyCombination accelerator, boolean disable) {
        var item = new MenuItem(text);

        if (graphic != null) {
            item.setGraphic(new FontIcon(graphic));
        }
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.setDisable(disable);

        return item;
    }

    public static ToggleButton toggleButton(String text,
                                            Ikon icon,
                                            ToggleGroup group,
                                            boolean selected,
                                            String... styleClasses) {
        var toggleButton = new ToggleButton(text);
        if (icon != null) {
            toggleButton.setGraphic(new FontIcon(icon));
        }
        if (group != null) {
            toggleButton.setToggleGroup(group);
        }
        toggleButton.setSelected(selected);
        toggleButton.getStyleClass().addAll(styleClasses);

        return toggleButton;
    }

    public static Hyperlink hyperlink(String text, URI uri) {
        var hyperlink = new Hyperlink(text);
        if (uri != null) {
            hyperlink.setOnAction(event -> Hyperlinks.open(uri.toString()));
        }
        return hyperlink;
    }
}
