package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class LabelComp extends Comp<CompStructure<Label>> {

    private final ObservableValue<String> text;
    private final ObservableValue<LabelGraphic> graphic;

    public LabelComp(String text, LabelGraphic graphic) {
        this(new SimpleStringProperty(text), new SimpleObjectProperty<>(graphic));
    }

    public LabelComp(String text) {
        this(new SimpleStringProperty(text));
    }

    public LabelComp(ObservableValue<String> text) {
        this(text, new SimpleObjectProperty<>());
    }

    @Override
    public CompStructure<Label> createBase() {
        var label = new Label();
        text.subscribe(t -> {
            PlatformThread.runLaterIfNeeded(() -> label.setText(t));
        });
        graphic.subscribe(t -> {
            PlatformThread.runLaterIfNeeded(() -> label.setGraphic(t != null ? t.createGraphicNode() : null));
        });
        label.setAlignment(Pos.CENTER);
        return new SimpleCompStructure<>(label);
    }
}
