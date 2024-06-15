package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;

import java.util.List;

public interface FixedServiceCreatorStore extends DataStore {

    List<? extends DataStoreEntryRef<? extends AbstractServiceStore>> createFixedServices() throws Exception;
}
