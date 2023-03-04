package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.ValidationException;
import org.apache.commons.lang3.function.FailablePredicate;

public class Validators {

    public static void nonNull(Object object, String name) throws ValidationException {
        if (object == null) {
            throw new ValidationException(AppI18n.get("app.mustNotBeEmpty", name));
        }
    }

    public static void notEmpty(String string, String name) throws ValidationException {
        if (string.trim().length() == 0) {
            throw new ValidationException(AppI18n.get("app.mustNotBeEmpty", name));
        }
    }

    public static void namedStoreExists(DataStore store, String name) throws ValidationException {
        if (!DataStorage.get().getUsableStores().contains(store)) {
            throw new ValidationException(AppI18n.get("app.missingStore", name));
        }
    }

    public static void hostFeature(ShellStore host, FailablePredicate<ShellStore, Exception> predicate, String name)
            throws Exception {
        if (!predicate.test(host)) {
            throw new ValidationException(AppI18n.get("app.hostFeatureUnsupported", name));
        }
    }
}
