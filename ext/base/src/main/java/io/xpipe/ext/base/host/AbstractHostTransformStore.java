package io.xpipe.ext.base.host;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.storage.DataStoreEntryRef;

public interface AbstractHostTransformStore extends DataStore {

    boolean canConvertToAbstractHost();

    AbstractHostStore createAbstractHostStore();

    AbstractHostTransformStore withNewParent(DataStoreEntryRef<AbstractHostStore> newParent);
}
