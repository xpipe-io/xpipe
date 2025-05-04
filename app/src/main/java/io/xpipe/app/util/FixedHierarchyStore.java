package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;

import java.util.List;

public interface FixedHierarchyStore extends DataStore {

    default boolean canManuallyRefresh() {
        return true;
    }

    default boolean removeLeftovers() {
        return true;
    }

    List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren() throws Exception;
}
