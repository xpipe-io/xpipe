package io.xpipe.extension.comp;

import com.jfoenix.controls.JFXTooltip;
import io.xpipe.extension.I18n;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.Shortcuts;
import io.xpipe.fxcomps.augment.Augment;
import io.xpipe.fxcomps.util.PlatformThread;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Duration;

public class FancyTooltipAugment<S extends CompStructure<?>> implements Augment<S> {

    static {
        JFXTooltip.setHoverDelay(Duration.millis(400));
        JFXTooltip.setVisibleDuration(Duration.INDEFINITE);
    }

    private final ObservableValue<String> text;

    public FancyTooltipAugment(ObservableValue<String> text) {
        this.text = PlatformThread.sync(text);
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
            if (owner == null || owner.isFocused()) {
                super.show();
            }
        }

        @Override
        public void hide() {
            Window owner = getOwnerWindow();
            if (owner == null || owner.isFocused()) {
                try {
                    super.hide();
                } catch (Exception e) {
                }
            }
        }

        @Override
        public void show(Node ownerNode, double anchorX, double anchorY) {
            Window owner = getOwnerWindow();
            if (owner == null || owner.isFocused()) {
                super.show(ownerNode, anchorX, anchorY);
            }
        }

        @Override
        public void showOnAnchors(Node ownerNode, double anchorX, double anchorY) {
            Window owner = getOwnerWindow();
            if (owner == null || owner.isFocused()) {
                super.showOnAnchors(ownerNode, anchorX, anchorY);
            }
        }

        @Override
        public void show(Window owner) {
            if (owner == null || owner.isFocused()) {
                super.show(owner);
            }
        }

        @Override
        public void show(Window ownerWindow, double anchorX, double anchorY) {
            Window owner = getOwnerWindow();
            if (owner == null || owner.isFocused()) {
                super.show(ownerWindow, anchorX, anchorY);
            }
        }
    }
}
