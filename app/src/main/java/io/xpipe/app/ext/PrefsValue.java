package io.xpipe.app.ext;

public interface PrefsValue {

    default boolean isAvailable() {
        return true;
    }

    default boolean isSelectable() {
        return true;
    }
}
