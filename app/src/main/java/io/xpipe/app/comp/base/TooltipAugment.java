package io.xpipe.app.comp.base;

import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.augment.Augment;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;

public class TooltipAugment<S extends CompStructure<?>> implements Augment<S> {

    private final ObservableValue<String> text;
    private final KeyCombination shortcut;

    public TooltipAugment(ObservableValue<String> text, KeyCombination shortcut) {
        this.text = text;
        this.shortcut = shortcut;
    }

    public TooltipAugment(String key, KeyCombination shortcut) {
        this.text = AppI18n.observable(key);
        this.shortcut = shortcut;
    }

    @Override
    public void augment(S struc) {
        var tt = new FixedTooltip();
        if (shortcut != null) {
            var s = AppI18n.observable("shortcut");
            var binding = Bindings.createStringBinding(
                    () -> {
                        return text.getValue() + "\n\n" + s.getValue() + ": " + shortcut.getDisplayText();
                    },
                    text,
                    s);
            tt.textProperty().bind(PlatformThread.sync(binding));
        } else {
            tt.textProperty().bind(PlatformThread.sync(text));
        }
        AppFontSizes.sm(tt.getStyleableNode());
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");
        Tooltip.install(struc.get(), tt);
    }

    private static class FixedTooltip extends Tooltip {

        public FixedTooltip() {
            super();
        }

        @Override
        protected void show() {
            Window owner = getOwnerWindow();
            if (owner.isFocused()) super.show();
        }
    }
}
