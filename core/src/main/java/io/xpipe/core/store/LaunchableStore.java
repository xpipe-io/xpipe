package io.xpipe.core.store;

import io.xpipe.core.process.ProcessControl;

public interface LaunchableStore extends DataStore {

    default boolean canLaunch() {
        return true;
    }

    ProcessControl prepareLaunchCommand() throws Exception;
}
