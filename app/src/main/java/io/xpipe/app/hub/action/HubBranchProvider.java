package io.xpipe.app.hub.action;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.List;

public interface HubBranchProvider<T extends DataStore> extends HubMenuItemProvider<T> {

    List<HubMenuItemProvider<?>> getChildren(DataStoreEntryRef<T> store);
}
