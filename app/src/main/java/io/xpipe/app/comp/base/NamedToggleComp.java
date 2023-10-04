package io.xpipe.app.comp.base;

import atlantafx.base.controls.ToggleSwitch;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

public class NamedToggleComp extends SimpleComp {

    private final BooleanProperty selected;
    private final ObservableValue<String> name;

    public NamedToggleComp(BooleanProperty selected, ObservableValue<String> name) {
        this.selected = selected;
        this.name = name;
    }

    @Override
    protected Region createSimple() {
        var s = new ToggleSwitch();
        s.setSelected(selected.getValue());
        s.selectedProperty().addListener((observable, oldValue, newValue) -> {
            selected.set(newValue);
        });
        selected.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                s.setSelected(newValue);
            });
        });
        if (name != null) {
            s.textProperty().bind(PlatformThread.sync(name));
        }
        return s;
    }
}
