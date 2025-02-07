package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ValidationException;

import java.util.List;

public class Validators {

    public static <T extends DataStore> void isType(DataStoreEntryRef<? extends T> ref, Class<T> c) throws ValidationException {
        if (ref == null || !c.isAssignableFrom(ref.getStore().getClass())) {
            throw new ValidationException("Value must be an instance of " + c.getSimpleName());
        }
    }

    public static void nonNull(Object object) throws ValidationException {
        if (object == null) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }

    public static void nonNull(Object object, String name) throws ValidationException {
        if (object == null) {
            throw new ValidationException(AppI18n.get("app.mustNotBeEmpty", name));
        }
    }

    public static void contentNonNull(List<?> object) throws ValidationException {
        if (object.stream().anyMatch(o -> o == null)) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }

    public static void notEmpty(String string) throws ValidationException {
        if (string.trim().length() == 0) {
            throw new ValidationException(AppI18n.get("app.valueMustNotBeEmpty"));
        }
    }
}
