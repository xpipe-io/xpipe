package io.xpipe.extension.comp;

import com.jfoenix.controls.JFXTooltip;
import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.Shortcuts;
import io.xpipe.fxcomps.augment.Augment;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Supplier;

public class FancyTooltipAugment<S extends CompStructure<?>> implements Augment<S> {

    static {
        JFXTooltip.setHoverDelay(Duration.millis(400));
        JFXTooltip.setVisibleDuration(Duration.INDEFINITE);
    }

    private final ObservableValue<String> text;

    public FancyTooltipAugment(Supplier<String> text) {
        this.text = new SimpleObjectProperty<>(text.get());
    }

    public FancyTooltipAugment(String key) {
        this.text = I18n.observable(key);
    }

    @Override
    public void augment(S struc) {
        var tt = new FocusTooltip();
        var toDisplay = text.getValue();
        if (Shortcuts.getShortcut(struc.get()) != null) {
            toDisplay = toDisplay + " (" + Shortcuts.getShortcut(struc.get()).getDisplayText() + ")";
        }
        tt.textProperty().setValue(toDisplay);
        tt.setStyle("-fx-font-size: 11pt;");
        JFXTooltip.install(struc.get(), tt);
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");
    }


    public void augment(Node region) {
        var tt = new FocusTooltip();
        var toDisplay = text.getValue();
        if (Shortcuts.getShortcut((Region) region) != null) {
            toDisplay = toDisplay + " (" + Shortcuts.getShortcut((Region) region).getDisplayText() + ")";
        }
        tt.textProperty().setValue(toDisplay);
        tt.setStyle("-fx-font-size: 11pt;");
        JFXTooltip.install(region, tt);
        tt.setWrapText(true);
        tt.setMaxWidth(400);
        tt.getStyleClass().add("fancy-tooltip");
    }

    private static class FocusTooltip extends JFXTooltip {

        public FocusTooltip() {
        }

        public FocusTooltip(String string) {
            super(string);
        }

        @Override
        protected void show() {
            Window owner = getOwnerWindow();
            if (owner.isFocused())
                super.show();
        }
    }
}
