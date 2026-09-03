package io.xpipe.ext.base.store;

import io.xpipe.app.store.DataStore;

public interface StoppableStore extends DataStore {

    void stop() throws Exception;
}
