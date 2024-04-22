package io.xpipe.core.store;

public interface ExpandedLifecycleStore extends DataStore {

    default void initializeValidate() {}

    default void finalizeValidate() throws Exception {}
}
