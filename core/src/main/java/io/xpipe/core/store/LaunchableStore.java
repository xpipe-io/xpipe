package io.xpipe.core.store;

public interface LaunchableStore extends DataStore {

    String prepareLaunchCommand(String displayName) throws Exception;
}
