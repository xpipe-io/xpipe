package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.DataStore;

import java.util.List;

public class FixedServiceGroupStoreProvider extends AbstractServiceGroupStoreProvider {

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return FixedServiceGroupStore.builder().build();
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        FixedServiceGroupStore s = store.getStore().asNeeded();
        return s.getParent().get();
    }

    @Override
    public String getId() {
        return "fixedServiceGroup";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(FixedServiceGroupStore.class);
    }
}
