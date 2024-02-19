package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppFont;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.util.InPlaceSecretValue;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Objects;

public class SecretFieldComp extends Comp<CompStructure<TextField>> {

    public static SecretFieldComp ofString(Property<String> s) {
        var prop = new SimpleObjectProperty<>(s.getValue() != null ? InPlaceSecretValue.of(s.getValue()) : null);
        prop.addListener((observable, oldValue, newValue) -> {
            s.setValue(newValue != null ? new String(newValue.getSecret()) : null);
        });
        s.addListener((observableValue, s1, t1) -> {
            prop.set(t1 != null ? InPlaceSecretValue.of(t1) : null);
        });
        return new SecretFieldComp(prop);
    }

    private final Property<InPlaceSecretValue> value;

    public SecretFieldComp(Property<InPlaceSecretValue> value) {
        this.value = value;
    }

    protected InPlaceSecretValue encrypt(char[] c) {
        return InPlaceSecretValue.of(c);
    }

    @Override
    public CompStructure<TextField> createBase() {
        var text = new PasswordField();
        text.getStyleClass().add("secret-field-comp");
        text.setText(value.getValue() != null ? value.getValue().getSecretValue() : null);
        text.textProperty().addListener((c, o, n) -> {
            value.setValue(n != null && n.length() > 0 ? encrypt(n.toCharArray()) : null);
        });
        value.addListener((c, o, n) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                // Check if control value is the same. Then don't set it as that might cause bugs
                if ((n == null && text.getText().isEmpty()) || Objects.equals(text.getText(), n != null ? n.getSecretValue() : null)) {
                    return;
                }

                text.setText(n != null ? n.getSecretValue() : null);
            });
        });
        AppFont.small(text);
        return new SimpleCompStructure<>(text);
    }
}
