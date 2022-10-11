package io.xpipe.extension.util;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.I18n;
import javafx.beans.value.ObservableValue;
import net.synedra.validatorfx.Check;

import java.util.function.Predicate;

public class Validators {

    public static Check nonNull(Validator v, ObservableValue<String> name, ObservableValue<?> s) {
        return v.createCheck().dependsOn("val", s).withMethod(c -> {
            if (c.get("val") == null) {
                c.error(I18n.get("extension.mustNotBeEmpty", name.getValue()));
            }
        });
    }

    public static void nonNull(Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(I18n.get("extension.null", name));
        }
    }

    public static void namedStoreExists(DataStore store, String name) {
        if (!XPipeDaemon.getInstance().getNamedStores().contains(store) && !(store instanceof LocalStore)) {
            throw new IllegalArgumentException(I18n.get("extension.missingStore", name));
        }
    }

    public static void hostFeature(ShellStore host, Predicate<ShellStore> predicate, String name) {
        if (!predicate.test(host)) {
            throw new IllegalArgumentException(I18n.get("extension.hostFeatureUnsupported", name));
        }
    }
}
