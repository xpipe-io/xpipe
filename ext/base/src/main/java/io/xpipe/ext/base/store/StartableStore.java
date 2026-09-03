package io.xpipe.ext.base.store;

import io.xpipe.app.store.DataStore;

public interface StartableStore extends DataStore {

    void start() throws Exception;
}
