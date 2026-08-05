package io.xpipe.app.util;

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
