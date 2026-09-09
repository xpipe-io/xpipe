package io.xpipe.ext.base.store;

import io.xpipe.app.store.DataStore;

public interface ForceStoppableStore extends DataStore {

    void forceStop() throws Exception;
}
