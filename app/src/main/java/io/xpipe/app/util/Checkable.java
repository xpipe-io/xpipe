package io.xpipe.app.util;

import io.xpipe.app.ext.ValidationException;

public interface Checkable {

    void checkComplete() throws ValidationException;

    default boolean isComplete() {
        try {
            checkComplete();
            return true;
        } catch (ValidationException ignored) {
            return false;
        }
    }
}
