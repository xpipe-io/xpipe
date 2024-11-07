package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

public class LabelComp extends Comp<CompStructure<Label>> {

    private final ObservableValue<String> text;

    public LabelComp(String text) {
        this.text = new SimpleStringProperty(text);
    }

    public LabelComp(ObservableValue<String> text) {
        this.text = text;
    }

    @Override
    public CompStructure<Label> createBase() {
        var label = new Label();
        text.subscribe(t -> {
            PlatformThread.runLaterIfNeeded(() -> label.setText(t));
        });
        label.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(label);
    }
}
