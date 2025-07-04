package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.ext.DataStore;

import java.util.List;

public interface FixedServiceCreatorStore extends DataStore {

    default boolean allowManualServicesRefresh() {
        return true;
    }

    List<? extends DataStoreEntryRef<? extends AbstractServiceStore>> createFixedServices() throws Exception;
}
