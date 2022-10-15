package io.xpipe.extension.comp;

import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class TextFieldComp extends Comp<CompStructure<TextField>> {

    private final Property<String> value;
    private final Property<String> lazyValue;

    public TextFieldComp(Property<String> value) {
        this.value = value;
        this.lazyValue = value;
    }

    public TextFieldComp(Property<String> value, Property<String> lazyValue) {
        this.value = value;
        this.lazyValue = lazyValue;
    }

    @Override
    public CompStructure<TextField> createBase() {
        var text = new TextField(value.getValue() != null ? value.getValue().toString() : null);
        text.textProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? n : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n);
            });
        });
        text.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent ke) {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    lazyValue.setValue(value.getValue());
                }
                ke.consume();
            }
        });
        return new SimpleCompStructure<>(text);
    }
}
