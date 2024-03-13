package io.xpipe.core.store;

public interface LaunchableStore extends DataStore {

    default void launch() throws Exception {}
}
