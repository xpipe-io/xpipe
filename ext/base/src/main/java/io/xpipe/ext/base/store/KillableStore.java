package io.xpipe.ext.base.store;

import io.xpipe.app.store.DataStore;

public interface KillableStore extends DataStore {

    void kill() throws Exception;
}
