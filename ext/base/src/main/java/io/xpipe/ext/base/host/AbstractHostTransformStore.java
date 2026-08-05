package io.xpipe.ext.base.host;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;

public interface AbstractHostTransformStore extends DataStore {

    boolean canConvertToAbstractHost();

    AbstractHostStore createAbstractHostStore();

    AbstractHostTransformStore withNewParent(DataStoreEntryRef<AbstractHostStore> newParent);
}
