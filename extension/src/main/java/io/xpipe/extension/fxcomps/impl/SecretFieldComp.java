package io.xpipe.extension.fxcomps.impl;

import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import javafx.beans.property.Property;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SecretFieldComp extends Comp<CompStructure<TextField>> {

    private final Property<SecretValue> value;

    public SecretFieldComp(Property<SecretValue> value) {
        this.value = value;
    }

    @Override
    public CompStructure<TextField> createBase() {
        var text = new PasswordField();
        text.setText(value.getValue() != null ? value.getValue().getSecretValue() : null);
        text.textProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? SecretValue.create(n) : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n != null ? n.getSecretValue() : null);
            });
        });
        return new SimpleCompStructure<>(text);
    }
}
