package io.xpipe.fxcomps.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
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
        this.text = PlatformThread.sync(text);
    }

    @Override
    public CompStructure<Label> createBase() {
        var label = new Label();
        label.textProperty().bind(text);
        label.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(label);
    }
}
