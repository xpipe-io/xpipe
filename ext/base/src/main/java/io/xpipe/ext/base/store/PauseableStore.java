package io.xpipe.ext.base.store;

import io.xpipe.core.store.DataStore;

public interface PauseableStore extends DataStore {

    void pause() throws Exception;
}
