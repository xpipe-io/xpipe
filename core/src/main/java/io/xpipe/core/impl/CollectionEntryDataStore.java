package io.xpipe.core.impl;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FilenameStore;
import io.xpipe.core.store.StreamDataStore;

public abstract class CollectionEntryDataStore implements FilenameStore, StreamDataStore {

    private final boolean directory;
    private final String name;
    private final DataStore collectionStore;

    public CollectionEntryDataStore(boolean directory, String name, DataStore collectionStore) {
        this.directory = directory;
        this.name = name;
        this.collectionStore = collectionStore;
    }

    @Override
    public String getFileName() {
        return name;
    }

    public boolean isDirectory() {
        return directory;
    }

    public DataStore getCollectionStore() {
        return collectionStore;
    }

    public String getName() {
        return name;
    }

    public String getPreferredProvider() {
        return null;
    }
}
