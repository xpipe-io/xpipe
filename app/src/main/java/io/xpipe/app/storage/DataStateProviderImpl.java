package io.xpipe.app.storage;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.DataStateProvider;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public class DataStateProviderImpl extends DataStateProvider {

    @Override
    public void putState(DataStore store, String key, Object value) {
        if (DataStorage.get() == null) {
            return;
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(store);
        if (entry.isEmpty()) {
            return;
        }

        var old = entry.get().getElementState().put(key, value);
        if (!Objects.equals(old, value)) {
            entry.get().simpleRefresh();
        }
    }

    @Override
    public <T> T getState(DataStore store, String key, Class<T> c, Supplier<T> def) {
        if (DataStorage.get() == null) {
            return def.get();
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(store);
        if (entry.isEmpty()) {
            return def.get();
        }

        var result = entry.get().getElementState().computeIfAbsent(key, k -> def.get());
        return c.cast(result);
    }

    @Override
    public Path getInternalStreamStore(UUID id) {
        return DataStorage.get().getInternalStreamPath(id);
    }
}
