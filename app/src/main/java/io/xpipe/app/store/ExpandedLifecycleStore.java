package io.xpipe.app.store;

public interface ExpandedLifecycleStore extends DataStore {

    default void finalizeStore() throws Exception {}
}
