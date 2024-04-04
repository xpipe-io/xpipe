package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.augment.Augment;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.Shortcuts;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Tooltip;

public class TooltipAugment<S extends CompStructure<?>> implements Augment<S> {

    private final ObservableValue<String> text;

    public TooltipAugment(ObservableValue<String> text) {
        this.text = PlatformThread.sync(text);
    }

    public TooltipAugment(String key) {
        this.text = AppI18n.observable(key);
    }

    @Override
    public void augment(S struc) {
        var region = struc.get();
        var tt = new Tooltip();
        if (Shortcuts.getDisplayShortcut(region) != null) {
            var s = AppI18n.observable("shortcut");
            var binding = Bindings.createStringBinding(() -> {
                return text.getValue() + "\n\n" + s.getValue() + ": " + Shortcuts.getDisplayShortcut(region).getDisplayText();
            }, text, s);
            tt.textProperty().bind(binding);
        } else {
            tt.textProperty().bind(text);
        }
        tt.setStyle("-fx-font-size: 11pt;");
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");

        Tooltip.install(struc.get(), tt);
    }
}
