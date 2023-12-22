package io.xpipe.app.storage;

import io.xpipe.core.store.DataStore;
import lombok.NonNull;
import lombok.Value;

@Value
public class DataStoreEntryRef<T extends DataStore> {

    @NonNull
    DataStoreEntry entry;

    public DataStoreEntryRef(@NonNull DataStoreEntry entry) {
        this.entry = entry;
    }

    public void checkComplete() throws Throwable {
        getStore().checkComplete();
    }

    public DataStoreEntry get() {
        return entry;
    }

    public T getStore() {
        return entry.getStore().asNeeded();
    }
}
