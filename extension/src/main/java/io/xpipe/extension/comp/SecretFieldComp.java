package io.xpipe.extension.comp;

import io.xpipe.core.util.SecretValue;
import io.xpipe.fxcomps.Comp;
import io.xpipe.fxcomps.CompStructure;
import io.xpipe.fxcomps.SimpleCompStructure;
import io.xpipe.fxcomps.util.PlatformThread;
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
            value.setValue(n != null && n.length() > 0 ? SecretValue.createForSecretValue(n) : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                text.setText(n != null ? n.getSecretValue() : null);
            });
        });
        return new SimpleCompStructure<>(text);
    }
}
