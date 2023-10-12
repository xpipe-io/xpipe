package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;

import java.util.List;

public interface FixedHierarchyStore extends DataStore {

    List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren(DataStoreEntry self) throws Exception;
}
