package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;

import java.util.List;

public class ServiceGroupStoreProvider extends AbstractServiceGroupStoreProvider {

    @Override
    public DataStore defaultStore() {
        return ServiceGroupStore.builder().build();
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        ServiceGroupStore s = store.getStore().asNeeded();
        return s.getParent().get();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("serviceGroup");
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(ServiceGroupStore.class);
    }
}
