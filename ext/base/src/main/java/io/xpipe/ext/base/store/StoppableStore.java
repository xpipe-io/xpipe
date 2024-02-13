package io.xpipe.ext.base.store;

import io.xpipe.core.store.DataStore;

public interface StoppableStore extends DataStore {

    void stop() throws Exception;
}
