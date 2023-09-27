package io.xpipe.app.util;

import io.xpipe.core.store.DataStore;

public interface StatefulDataStore<T> extends DataStore {

    default T getState() {
        return null;
    }

    default void setState(T val) {
    }
}
