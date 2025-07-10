package io.xpipe.ext.base.store;

import io.xpipe.app.ext.DataStore;

public interface PauseableStore extends DataStore {

    void pause() throws Exception;
}
