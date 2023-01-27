package io.xpipe.app.storage;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.DataStateProvider;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

public class DataStateProviderImpl extends DataStateProvider {

    @Override
    public void putState(DataStore store, String key, Object value) {
        var entry = DataStorage.get().getEntryByStore(store);
        if (entry.isEmpty()) {
            return;
        }

        entry.get().getElementState().put(key, value);
        entry.get().simpleRefresh();
    }

    @Override
    public <T> T getState(DataStore store, String key, Class<T> c, Supplier<T> def) {
        var entry = DataStorage.get().getEntryByStore(store);
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
