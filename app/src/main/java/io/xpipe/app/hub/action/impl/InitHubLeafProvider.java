package io.xpipe.app.hub.action.impl;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;

public abstract class InitHubLeafProvider<T extends DataStore, O> implements HubLeafProvider<T> {

    protected O available;

    @Override
    public void init() {
        ThreadHelper.runFailableAsync(() -> {
            available = check();
            StoreViewState.get().updateWrappers();
        });
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<T> o) {
        return available != null;
    }

    protected abstract O check() throws Exception;
}
