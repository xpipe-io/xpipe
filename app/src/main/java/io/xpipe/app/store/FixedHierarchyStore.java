package io.xpipe.app.store;

import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.List;

public interface FixedHierarchyStore extends DataStore {

    default boolean canManuallyRefresh() {
        return true;
    }

    default boolean removeLeftovers() {
        return true;
    }

    default void enableShowAll() {}

    List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren() throws Exception;
}
