package io.xpipe.core.store;

import io.xpipe.core.process.CommandProcessControl;

public interface CommandExecutionStore extends DataStore {

    CommandProcessControl create() throws Exception;
}
