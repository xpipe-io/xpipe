package io.xpipe.app.action;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import java.util.List;

public interface BranchStoreActionProvider<T extends DataStore> extends StoreActionProvider<T> {

    List<? extends ActionProvider> getChildren(DataStoreEntryRef<T> store);
}
