package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleChooserControl;
import io.xpipe.app.core.AppI18n;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.StringConverter;

import java.nio.file.Path;
import java.util.Objects;

public class PrefFields {

    public static StringField ofPath(ObjectProperty<Path> fileProperty) {
        StringProperty stringProperty = new SimpleStringProperty();
        stringProperty.bindBidirectional(fileProperty, new StringConverter<Path>() {
            @Override
            public String toString(Path file) {
                if (Objects.isNull(file)) {
                    return "";
                }
                return file.toString();
            }

            @Override
            public Path fromString(String value) {
                return Path.of(value);
            }
        });
        return StringField.ofStringType(stringProperty)
                .render(() -> new SimpleChooserControl(
                        AppI18n.get("browse"), fileProperty.getValue().toFile(), true));
    }
}
