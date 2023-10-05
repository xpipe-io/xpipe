package io.xpipe.ext.base;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;

import java.util.UUID;

public interface SelfReferentialStore extends DataStore {

    default DataStoreEntry getSelfEntry() {
        return DataStorage.get().getStoreEntries().stream().filter(dataStoreEntry -> dataStoreEntry.getStore() == this).findFirst().orElseGet(() -> {
            return DataStoreEntry.createNew(UUID.randomUUID(),DataStorage.DEFAULT_CATEGORY_UUID, "Invalid", this);
        });
    }
}
