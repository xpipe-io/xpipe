package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.util.PlatformUtil;
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
            PlatformUtil.runLaterIfNeeded(() -> {
                text.setText(n != null ? n.toString() : null);
            });
        });
        return new CompStructure<>(text);
    }
}
