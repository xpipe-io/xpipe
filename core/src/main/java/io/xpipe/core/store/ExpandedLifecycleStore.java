package io.xpipe.core.store;

public interface ExpandedLifecycleStore extends DataStore{

    default void initializeValidate() throws Exception {}

    default void finalizeValidate() throws Exception {}
}
