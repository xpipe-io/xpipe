package io.xpipe.ext.base.store;

import io.xpipe.core.store.DataStore;

public interface StartableStore extends DataStore {

    void start() throws Exception;
}
