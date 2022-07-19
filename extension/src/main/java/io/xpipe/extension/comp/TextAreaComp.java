package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.scene.control.TextArea;

public class TextAreaComp extends Comp<CompStructure<TextArea>> {

    private final Property<String> value;

    public TextAreaComp(Property<String> value) {
        this.value = value;
    }

    @Override
    public CompStructure<TextArea> createBase() {
        var text = new TextArea(value.getValue() != null ? value.getValue().toString() : null);
        text.textProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? n : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n);
            });
        });
        return new SimpleCompStructure<>(text);
    }
}
