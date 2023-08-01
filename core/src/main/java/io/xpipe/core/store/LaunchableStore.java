package io.xpipe.core.store;

public interface LaunchableStore extends DataStore {

    default boolean canLaunch() {
        return true;
    }

    String prepareLaunchCommand(String displayName) throws Exception;
}
