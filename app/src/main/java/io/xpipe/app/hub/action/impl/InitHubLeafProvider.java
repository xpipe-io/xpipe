package io.xpipe.app.hub.action.impl;

import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.ext.DataStore;

public abstract class InitHubLeafProvider<T extends DataStore, O> implements HubLeafProvider<T> {

    protected O available;

    @Override
    public void init() throws Exception {
        ThreadHelper.runFailableAsync(() -> {
            available = check();

            // Update entries to potentially show item
            if (available != null) {
                StoreViewState.get().getAllEntries().getList().stream().filter(w -> w.getValidity().getValue().isUsable()).forEach(w -> {
                    if (getApplicableClass().isAssignableFrom(w.getStore().getValue().getClass())) {
                        w.update();
                    }
                });
            }
        });
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<T> o) {
        return available != null;
    }

    protected abstract O check() throws Exception;
}
