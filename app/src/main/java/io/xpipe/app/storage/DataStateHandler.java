package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreState;
import io.xpipe.app.ext.StatefulDataStore;

import java.util.function.Supplier;

public class DataStateHandler {

    private static final DataStateHandler INSTANCE = new DataStateHandler();

    public static DataStateHandler get() {
        return INSTANCE;
    }

    public void setState(DataStore store, DataStoreState value) {
        if (DataStorage.get() == null) {
            return;
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(store, true).or(() -> DataStorage.get()
                .getStoreEntryInProgressIfPresent(store));
        if (entry.isEmpty()) {
            return;
        }

        entry.get().setStorePersistentState(value);
    }

    public <T extends DataStoreState> T getState(DataStore store, Supplier<T> def) {
        if (DataStorage.get() == null) {
            return def.get();
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(store, true).or(() -> DataStorage.get()
                .getStoreEntryInProgressIfPresent(store));
        if (entry.isEmpty()) {
            return def.get();
        }

        if (!(store instanceof StatefulDataStore<?>)) {
            return def.get();
        }

        var found = entry.get().getStorePersistentState();
        if (found == null) {
            entry.get().setStorePersistentState(def.get());
        }
        T r = entry.get().getStorePersistentState();
        return r != null ? r : def.get();
    }

    public void putCache(DataStore store, String key, Object value) {
        if (DataStorage.get() == null) {
            return;
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(store, true).or(() -> DataStorage.get()
                .getStoreEntryInProgressIfPresent(store));
        if (entry.isEmpty()) {
            return;
        }

        entry.get().setStoreCache(key, value);
    }

    public <T> T getCache(DataStore store, String key, Class<T> c, Supplier<T> def) {
        if (DataStorage.get() == null) {
            return def.get();
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(store, true).or(() -> DataStorage.get()
                .getStoreEntryInProgressIfPresent(store));
        if (entry.isEmpty()) {
            return def.get();
        }

        var r = entry.get().getStoreCache().get(key);
        if (r == null) {
            r = def.get();
            entry.get().setStoreCache(key, r);
        }
        return c.cast(r);
    }

    public boolean canCacheToStorage(DataStore store) {
        var entry = DataStorage.get().getStoreEntryIfPresent(store, true).or(() -> DataStorage.get()
                .getStoreEntryInProgressIfPresent(store));
        return entry.isPresent();
    }
}
