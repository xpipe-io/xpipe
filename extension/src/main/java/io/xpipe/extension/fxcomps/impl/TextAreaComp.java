package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextArea;

public class TextAreaComp extends Comp<CompStructure<TextArea>> {

    private final Property<String> value;
    private final Property<String> lazyValue = new SimpleStringProperty();
    private final boolean lazy;

    public TextAreaComp(Property<String> value) {
        this(value, false);
    }

    public TextAreaComp(Property<String> value, boolean lazy) {
        this.value = value;
        this.lazy = lazy;
        if (!lazy) {
            value.bind(lazyValue);
        }
    }

    @Override
    public CompStructure<TextArea> createBase() {
        var text = new TextArea(value.getValue() != null ? value.getValue() : null);
        text.textProperty().addListener((c, o, n) -> {
            lazyValue.setValue(n != null && n.length() > 0 ? n : null);
        });
        value.addListener((c, o, n) -> {
            lazyValue.setValue(n);
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n);
            });
        });


        text.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                value.setValue(lazyValue.getValue());
            }
        });

        return new SimpleCompStructure<>(text);
    }
}
