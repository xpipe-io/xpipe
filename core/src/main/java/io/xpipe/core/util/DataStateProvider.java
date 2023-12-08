package io.xpipe.core.util;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.DataStoreState;

import java.util.ServiceLoader;
import java.util.function.Supplier;

public abstract class DataStateProvider {

    private static DataStateProvider INSTANCE;

    public static DataStateProvider get() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(ModuleLayer.boot(), DataStateProvider.class)
                    .findFirst()
                    .orElseThrow();
        }

        return INSTANCE;
    }

    public abstract void setState(DataStore store, DataStoreState value);

    public abstract <T extends DataStoreState> T getState(DataStore store, Supplier<T> def);

    public abstract void putCache(DataStore store, String key, Object value);

    public abstract <T> T getCache(DataStore store, String key, Class<T> c, Supplier<T> def);

    public abstract boolean isInStorage(DataStore store);
}
