package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.core.util.ValidationException;

public class Validators {

    public static void nonNull(Object object) throws ValidationException {
        if (object == null) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }

    public static void notEmpty(String string) throws ValidationException {
        if (string.trim().length() == 0) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }

    public static void namedStoreExists(DataStore store) throws ValidationException {
        if (!DataStorage.get().getUsableStores().contains(store)) {
            throw new ValidationException(AppI18n.get("app.missingStore"));
        }
    }

    public static void hostFeature(ShellStore host, FailableFunction<ShellStore, Boolean, Exception> predicate, String name)
            throws Exception {
        if (!predicate.apply(host)) {
            throw new ValidationException(AppI18n.get("app.hostFeatureUnsupported", name));
        }
    }
}
