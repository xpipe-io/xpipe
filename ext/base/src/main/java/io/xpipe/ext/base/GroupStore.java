package io.xpipe.ext.base;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

public interface GroupStore<T extends DataStore> extends DataStore {

    DataStoreEntryRef<T> getParent();

    @Override
    default void checkComplete() throws Exception {
        var p = getParent();
        if (p != null) {
            p.checkComplete();
        }
    }
}
