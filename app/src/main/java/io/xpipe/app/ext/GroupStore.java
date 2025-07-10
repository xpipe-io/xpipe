package io.xpipe.app.ext;

import io.xpipe.app.storage.DataStoreEntryRef;

public interface GroupStore<T extends DataStore> extends DataStore {

    DataStoreEntryRef<? extends T> getParent();

    @Override
    default void checkComplete() throws Throwable {
        var p = getParent();
        if (p != null) {
            p.checkComplete();
        }
    }
}
