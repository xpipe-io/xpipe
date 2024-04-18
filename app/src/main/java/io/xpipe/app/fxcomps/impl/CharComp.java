package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;

import javafx.beans.property.Property;
import javafx.scene.control.TextField;

public class CharComp extends Comp<CompStructure<TextField>> {

    private final Property<Character> value;

    public CharComp(Property<Character> value) {
        this.value = value;
    }

    @Override
    public CompStructure<TextField> createBase() {
        var text = new TextField(value.getValue() != null ? value.getValue().toString() : null);
        text.setOnKeyTyped(e -> {
            text.setText(e.getCharacter());
        });
        text.textProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? n.charAt(0) : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n != null ? n.toString() : null);
            });
        });
        return new SimpleCompStructure<>(text);
    }
}
