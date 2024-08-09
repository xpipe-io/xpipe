package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.Augment;
import io.xpipe.app.fxcomps.util.PlatformThread;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;
import javafx.util.Duration;

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
        tt.setStyle("-fx-font-size: 11pt;");
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");
        tt.setHideDelay(Duration.INDEFINITE);
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
