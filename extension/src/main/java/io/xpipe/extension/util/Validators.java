package io.xpipe.extension.util;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.ValidationException;
import io.xpipe.extension.I18n;

import java.util.function.Predicate;

public class Validators {

    public static void nonNull(Object object, String name) throws ValidationException {
        if (object == null) {
            throw new ValidationException(I18n.get("extension.mustNotBeEmpty", name));
        }
    }

    public static void notEmpty(String string, String name) throws ValidationException {
        if (string.trim().length() == 0) {
            throw new ValidationException(I18n.get("extension.mustNotBeEmpty", name));
        }
    }

    public static void namedStoreExists(DataStore store, String name) throws ValidationException {
        if (!XPipeDaemon.getInstance().getNamedStores().contains(store) && !(store instanceof LocalStore)) {
            throw new ValidationException(I18n.get("extension.missingStore", name));
        }
    }

    public static void hostFeature(ShellStore host, Predicate<ShellStore> predicate, String name)
            throws ValidationException {
        if (!predicate.test(host)) {
            throw new ValidationException(I18n.get("extension.hostFeatureUnsupported", name));
        }
    }
}
