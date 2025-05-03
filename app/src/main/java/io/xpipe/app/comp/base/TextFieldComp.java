package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.util.PlatformThread;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.Objects;

public class TextFieldComp extends Comp<CompStructure<TextField>> {

    private final Property<String> lastAppliedValue;
    private final Property<String> currentValue;
    private final boolean lazy;

    public TextFieldComp(Property<String> value) {
        this(value, false);
    }

    public TextFieldComp(Property<String> value, boolean lazy) {
        this.lastAppliedValue = value;
        this.currentValue = new SimpleStringProperty(value.getValue());
        this.lazy = lazy;
        if (!lazy) {
            currentValue.subscribe(val -> {
                value.setValue(val);
            });
        }
        lastAppliedValue.addListener((c, o, n) -> {
            currentValue.setValue(n);
        });
    }

    @Override
    public CompStructure<TextField> createBase() {
        var text = new TextField(currentValue.getValue() != null ? currentValue.getValue() : null);
        text.textProperty().addListener((c, o, n) -> {
            currentValue.setValue(n != null && n.length() > 0 ? n : null);
        });
        lastAppliedValue.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                // Check if control value is the same. Then don't set it as that might cause bugs
                if (Objects.equals(text.getText(), n)
                        || (n == null && text.getText().isEmpty())) {
                    return;
                }

                text.setText(n);
            });
        });

        text.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                text.getScene().getRoot().requestFocus();
                ke.consume();
            }

            if (lazy && ke.getCode().equals(KeyCode.ENTER)) {
                lastAppliedValue.setValue(currentValue.getValue());
                ke.consume();
            }
        });

        text.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && lazy) {
                lastAppliedValue.setValue(currentValue.getValue());
            }
        });

        return new SimpleCompStructure<>(text);
    }
}
