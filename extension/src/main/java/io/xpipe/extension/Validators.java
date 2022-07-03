package io.xpipe.extension;

import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;

public class Validators {

    public static Check nonNull(Validator v, ObservableValue<String> name, ObservableValue<?> s) {
        return v.createCheck().dependsOn("val", s).withMethod(c -> {
            if (c.get("val") == null ) {
                c.error(I18n.get("extension.mustNotBeEmpty", name.getValue()));
            }
        });
    }
}
