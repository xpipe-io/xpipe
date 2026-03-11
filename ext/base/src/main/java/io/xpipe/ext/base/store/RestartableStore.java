package io.xpipe.ext.base.store;

import io.xpipe.app.ext.DataStore;

public interface RestartableStore extends DataStore {

    void restart() throws Exception;
}
