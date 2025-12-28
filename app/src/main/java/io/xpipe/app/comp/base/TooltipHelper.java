package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppFontSizes;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;

public class TooltipHelper {

    public static Tooltip create(ObservableValue<String> text) {
        var tt = new FixedTooltip();
        tt.textProperty().bind(text);
        AppFontSizes.base(tt.getStyleableNode());
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");
        return tt;
    }

    private static class FixedTooltip extends Tooltip {

        public FixedTooltip() {
            super();
        }

        @Override
        protected void show() {
            Window owner = getOwnerWindow();
            if (owner.isFocused()) {
                super.show();
            }
        }
    }
}
