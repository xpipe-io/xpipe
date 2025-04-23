package io.xpipe.app.ext;

import io.xpipe.core.store.DataStore;

public interface NameableStore extends DataStore {

    String getName();
}
