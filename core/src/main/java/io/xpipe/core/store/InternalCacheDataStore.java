package io.xpipe.core.store;

import io.xpipe.core.util.DataStateProvider;

import java.util.function.Supplier;

public interface InternalCacheDataStore extends DataStore {

    default <T> T getCache(String key, Class<T> c, T def) {
        return DataStateProvider.get().getCache(this, key, c, () -> def);
    }

    default <T> T getOrCompute(String key, Class<T> c, Supplier<T> def) {
        return DataStateProvider.get().getCache(this, key, c, def);
    }

    default void setCache(String key, Object val) {
        DataStateProvider.get().putCache(this, key, val);
    }
}
