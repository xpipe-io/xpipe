package io.xpipe.app.hub.action;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import java.util.List;

public interface BranchStoreActionProvider<T extends DataStore> extends HubMenuItemProvider<T> {

    List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<T> store);
}
