package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStateHandler;

public interface InternalCacheDataStore extends DataStore {

    default <T> T getCache(String key, Class<T> c, T def) {
        return DataStateHandler.get().getCache(this, key, c, () -> def);
    }

    default void setCache(String key, Object val) {
        DataStateHandler.get().putCache(this, key, val);
    }

    default boolean canCacheToStorage() {
        return DataStateHandler.get().canCacheToStorage(this);
    }
}
