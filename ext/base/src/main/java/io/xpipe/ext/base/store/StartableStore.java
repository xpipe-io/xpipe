package io.xpipe.ext.base.store;

import io.xpipe.app.ext.DataStore;

public interface StartableStore extends DataStore {

    void start() throws Exception;
}
