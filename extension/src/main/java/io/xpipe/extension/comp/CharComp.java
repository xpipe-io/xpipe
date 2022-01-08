package io.xpipe.extension.comp;

import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.store.ValueStoreComp;
import javafx.scene.control.TextField;

public class CharComp extends ValueStoreComp<CompStructure<TextField>, Character> {

    @Override
    public CompStructure<TextField> createBase() {
        var text = new TextField(getValue() != null ? getValue().toString() : null);
        text.setOnKeyTyped(e -> {
            text.setText(e.getCharacter());
        });
        text.textProperty().addListener((c, o, n) -> {
            this.set(n != null && n.length() > 0 ? n.charAt(0) : null);
        });
        valueProperty().addListener((c, o, n) -> {
            text.setText(n != null ? n.toString() : null);
        });
        return new CompStructure<>(text);
    }
}
