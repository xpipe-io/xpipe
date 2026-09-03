package io.xpipe.app.store;

import io.xpipe.app.storage.DataStoreAccessScope;

public interface AccessScopeStore extends DataStore {

    DataStoreAccessScope getAccessScope();
}
