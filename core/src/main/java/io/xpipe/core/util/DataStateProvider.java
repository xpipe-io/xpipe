package io.xpipe.core.util;

import io.xpipe.core.store.DataStore;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.UUID;
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

    public abstract void putState(DataStore store, String key, Object value);

    public abstract <T> T getState(DataStore store, String key, Class<T> c, Supplier<T> def);

    public abstract Path getInternalStreamStore(UUID id);
}
