package io.xpipe.app.store;

import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.List;

public interface DependentDataStore extends DataStore {

    List<DataStoreEntryRef<?>> getDependencies();
}
