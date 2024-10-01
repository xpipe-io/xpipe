package io.xpipe.app.util;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.ValidatableStore;
import io.xpipe.core.store.ValidationContext;

import java.util.List;

public interface FixedHierarchyStore<T extends ValidationContext<?>> extends ValidatableStore<T>, DataStore {

    default boolean removeLeftovers() {
        return true;
    }

    @Override
    default T validate(T context) throws Exception {
        listChildren(context);
        return null;
    }

    List<? extends DataStoreEntryRef<? extends FixedChildStore>> listChildren(T context) throws Exception;
}
