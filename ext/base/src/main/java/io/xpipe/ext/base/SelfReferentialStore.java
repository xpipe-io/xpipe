package io.xpipe.ext.base;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;

import java.util.UUID;

public interface SelfReferentialStore extends DataStore {

    default DataStoreEntry getSelfEntry() {
        return DataStorage.get()
                .getStoreEntryIfPresent(this)
                .or(() -> {
                    return DataStorage.get().getStoreEntryInProgressIfPresent(this);
                })
                .orElseGet(() -> {
                    return DataStoreEntry.createNew(
                            UUID.randomUUID(), DataStorage.DEFAULT_CATEGORY_UUID, "Invalid", this);
                });
    }
}
