package io.xpipe.app.prefs;

public interface PrefsValue {

    default boolean isAvailable() {
        return true;
    }

    default boolean isSelectable() {
        return true;
    }
}
