package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.util.SecretValue;
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
            value.setValue(n != null && n.length() > 0 ? SecretValue.encrypt(n) : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n != null ? n.getSecretValue() : null);
            });
        });
        return new SimpleCompStructure<>(text);
    }
}
