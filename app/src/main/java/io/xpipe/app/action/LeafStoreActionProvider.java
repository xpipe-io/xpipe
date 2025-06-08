package io.xpipe.app.action;

import io.xpipe.app.hub.action.StoreActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

public interface LeafStoreActionProvider<T extends DataStore> extends StoreActionProvider<T> {

    default boolean isDefault(DataStoreEntryRef<T> o) {
        return false;
    }

    AbstractAction createAction(DataStoreEntryRef<T> ref);

    default boolean requiresValidStore() {
        return true;
    }

    default boolean isSystemAction() {
        return false;
    }
}
