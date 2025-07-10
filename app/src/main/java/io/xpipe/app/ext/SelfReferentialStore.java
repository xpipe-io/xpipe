package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface SelfReferentialStore extends DataStore {

    Map<DataStore, DataStoreEntry> FALLBACK = new HashMap<>();

    default DataStoreEntry getSelfEntry() {
        if (DataStorage.get() == null) {
            return DataStoreEntry.createTempWrapper(this);
        }

        return DataStorage.get()
                .getStoreEntryIfPresent(this, true)
                .or(() -> {
                    return DataStorage.get().getStoreEntryInProgressIfPresent(this);
                })
                .orElseGet(() -> {
                    synchronized (FALLBACK) {
                        var ex = FALLBACK.get(this);
                        if (ex != null) {
                            return ex;
                        }

                        var e = DataStoreEntry.createNew(
                                UUID.randomUUID(), DataStorage.DEFAULT_CATEGORY_UUID, "Invalid", this);
                        FALLBACK.put(this, e);
                        return e;
                    }
                });
    }
}
