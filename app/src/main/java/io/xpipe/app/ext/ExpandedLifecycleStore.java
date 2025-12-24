package io.xpipe.app.ext;

public interface ExpandedLifecycleStore extends DataStore {

    default void finalizeStore() throws Exception {}
}
