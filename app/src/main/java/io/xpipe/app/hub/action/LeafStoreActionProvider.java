package io.xpipe.app.hub.action;

import io.xpipe.app.action.AbstractAction;
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
}
