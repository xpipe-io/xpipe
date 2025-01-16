package io.xpipe.app.storage;

import io.xpipe.core.store.DataStore;

import lombok.NonNull;

import java.util.Objects;

public class DataStoreEntryRef<T extends DataStore> {

    @NonNull
    private final DataStoreEntry entry;

    public DataStoreEntryRef(@NonNull DataStoreEntry entry) {
        this.entry = entry;
    }

    @Override
    public int hashCode() {
        return entry.getUuid().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataStoreEntryRef<?> that)) {
            return false;
        }
        return Objects.equals(entry.getUuid(), that.entry.getUuid());
    }

    @Override
    public String toString() {
        return entry.getUuid().toString();
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

    @SuppressWarnings("unchecked")
    public <S extends DataStore> DataStoreEntryRef<S> asNeeded() {
        return (DataStoreEntryRef<S>) this;
    }
}
