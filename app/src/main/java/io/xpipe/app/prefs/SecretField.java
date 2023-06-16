package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.DataField;
import com.dlsc.formsfx.view.controls.SimplePasswordControl;
import io.xpipe.app.util.SecretHelper;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.Property;

public class SecretField
        extends DataField<Property<SecretValue>, SecretValue, com.dlsc.formsfx.model.structure.PasswordField> {

    protected SecretField(Property<SecretValue> valueProperty, Property<SecretValue> persistentValueProperty) {
        super(valueProperty, persistentValueProperty);

        stringConverter = new AbstractStringConverter<>() {
            @Override
            public SecretValue fromString(String string) {
                return SecretHelper.encrypt(string);
            }
        };
        rendererSupplier = () -> new SimplePasswordControl();

        userInput.set(stringConverter.toString(value.getValue()));
    }
}