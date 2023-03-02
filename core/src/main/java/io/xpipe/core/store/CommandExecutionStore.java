package io.xpipe.core.store;

import io.xpipe.core.process.CommandControl;

public interface CommandExecutionStore extends DataStore, LaunchableStore {

    @Override
    default String prepareLaunchCommand() throws Exception {
        return create().prepareTerminalOpen();
    }

    CommandControl create() throws Exception;
}
