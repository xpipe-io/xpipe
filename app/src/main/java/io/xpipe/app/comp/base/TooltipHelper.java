package io.xpipe.app.comp.base;

import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;

public class TooltipHelper {

    public static Tooltip create(String text) {
        return create(new SimpleStringProperty(text), null);
    }

    public static Tooltip create(ObservableValue<String> text, KeyCombination shortcut) {
        var tt = new FixedTooltip();
        if (shortcut != null) {
            var s = AppI18n.observable("shortcut");
            var binding = Bindings.createStringBinding(
                    () -> {
                        return text.getValue() + "\n\n" + s.getValue() + ": " + shortcut.getDisplayText();
                    },
                    text,
                    s);
            tt.textProperty().bind(binding);
        } else {
            tt.textProperty().bind(text);
        }
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
