package io.xpipe.core.store;

import io.xpipe.core.process.CommandControl;

public interface CommandExecutionStore extends DataStore, LaunchableStore {

    @Override
    default String prepareLaunchCommand(String displayName) throws Exception {
        return create().prepareTerminalOpen(displayName);
    }

    CommandControl create() throws Exception;
}
