package io.xpipe.core.store;

import io.xpipe.core.process.CommandProcessControl;

public interface CommandExecutionStore extends DataStore, LaunchableStore {

    @Override
    default String prepareLaunchCommand() throws Exception {
        return create().prepareTerminalOpen();
    }

    CommandProcessControl create() throws Exception;
}
