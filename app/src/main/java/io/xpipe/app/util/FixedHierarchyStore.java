package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;

import java.util.Map;

public interface FixedHierarchyStore extends DataStore {

    Map<String, FixedChildStore> listChildren(DataStoreEntry self) throws Exception;
}
