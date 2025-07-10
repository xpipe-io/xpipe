package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;

import java.util.List;

public class CustomServiceGroupStoreProvider extends AbstractServiceGroupStoreProvider {

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return CustomServiceGroupStore.builder().build();
    }

    @Override
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        CustomServiceGroupStore s = store.getStore().asNeeded();
        return s.getParent().get();
    }

    @Override
    public String getId() {
        return "customServiceGroup";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(CustomServiceGroupStore.class);
    }
}
