package io.xpipe.core.store;

import io.xpipe.core.util.DataStateProvider;

import java.util.function.Supplier;

public interface InternalCacheDataStore extends DataStore {

    default boolean isInStorage() {
        return DataStateProvider.get().isInStorage(this);
    }

    default <T> T getState(String key, Class<T> c, T def) {
        return DataStateProvider.get().getState(this, key, c, () -> def);
    }

    default <T> T getOrComputeState(String key, Class<T> c, Supplier<T> def) {
        return DataStateProvider.get().getState(this, key, c, def);
    }

    default void setState(String key, Object val) {
        DataStateProvider.get().putState(this, key, val);
    }
}
