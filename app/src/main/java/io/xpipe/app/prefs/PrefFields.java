package io.xpipe.app.prefs;

import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.preferencesfx.formsfx.view.controls.SimpleChooserControl;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;

public class PrefFields {

    public static StringField ofPath(ObjectProperty<Path> fileProperty) {
        StringProperty stringProperty = new SimpleStringProperty(fileProperty.getValue().toString());

        // Prevent garbage collection of this due to how preferencesfx handles properties via bindings
        BindingsHelper.linkPersistently(fileProperty, stringProperty);

        stringProperty.addListener((observable, oldValue, newValue) -> {
            fileProperty.setValue(newValue != null ? Path.of(newValue) : null);
        });

        fileProperty.addListener((observable, oldValue, newValue) -> {
            PlatformThread.runLaterIfNeeded(() -> {
                stringProperty.setValue(newValue != null ? newValue.toString() : "");
            });
        });

        return StringField.ofStringType(stringProperty)
                .render(() -> {
                    var c = new SimpleChooserControl(
                            AppI18n.get("browse"), fileProperty.getValue().toFile(), true);
                    c.setMinWidth(600);
                    c.setPrefWidth(600);
                    return c;
                });
    }
}
