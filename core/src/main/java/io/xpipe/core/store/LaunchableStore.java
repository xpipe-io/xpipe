package io.xpipe.core.store;

public interface LaunchableStore extends DataStore {

    String prepareLaunchCommand() throws Exception;
}
