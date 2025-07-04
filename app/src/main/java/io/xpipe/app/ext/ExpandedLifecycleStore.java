package io.xpipe.app.ext;

public interface ExpandedLifecycleStore extends DataStore {

    default void initializeStore() {}

    default void finalizeStore() throws Exception {}
}
