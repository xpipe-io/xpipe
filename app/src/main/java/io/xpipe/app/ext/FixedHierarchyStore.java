package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStoreEntryRef;

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
