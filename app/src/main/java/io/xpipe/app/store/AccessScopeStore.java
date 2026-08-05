package io.xpipe.app.store;

import io.xpipe.app.storage.DataStoreAccessScope;

public interface AccessScopeStore extends DataStore {

    default DataStore withUpdatedPrincipals() {
        return this;
    }

    DataStoreAccessScope getAccessScope();
}
