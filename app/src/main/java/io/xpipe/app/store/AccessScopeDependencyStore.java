package io.xpipe.app.store;

import io.xpipe.app.storage.DataStoreAccessScope;

public interface AccessScopeDependencyStore extends AccessScopeStore, DependentDataStore {

    default DataStoreAccessScope getAccessScope() {
        var deps = getDependencies();
        var scopes = deps.stream()
                .map(ref -> ref.getStore() instanceof AccessScopeStore s ? s.getAccessScope() : null)
                .filter(s -> s != null)
                .toList();
        if (scopes.isEmpty()) {
            return DataStoreAccessScope.encryption();
        }

        return DataStoreAccessScope.merge(scopes);
    }
}
