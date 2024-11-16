package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppFont;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;

public class PopupMenuButtonComp extends SimpleComp {

    private final ObservableValue<String> name;
    private final Comp<?> content;
    private final boolean lazy;

    public PopupMenuButtonComp(ObservableValue<String> name, Comp<?> content, boolean lazy) {
        this.name = name;
        this.content = content;
        this.lazy = lazy;
    }

    @Override
    protected Region createSimple() {
        var popover = new Popover();
        if (!lazy) {
            popover.setContentNode(content.createRegion());
        }
        popover.setCloseButtonEnabled(false);
        popover.setHeaderAlwaysVisible(false);
        popover.setDetachable(true);
        AppFont.small(popover.getContentNode());

        var extendedDescription = new Button();
        extendedDescription.textProperty().bind(name);
        extendedDescription.setMinWidth(Region.USE_PREF_SIZE);
        extendedDescription.getStyleClass().add(Styles.BUTTON_OUTLINED);
        extendedDescription.getStyleClass().add(Styles.ACCENT);
        extendedDescription.getStyleClass().add("long-description");
        extendedDescription.setOnAction(e -> {
            if (popover.isShowing()) {
                e.consume();
                return;
            }

            if (lazy) {
                popover.setContentNode(content.createRegion());
            }

            popover.show(extendedDescription);
            e.consume();
        });
        return extendedDescription;
    }
}
