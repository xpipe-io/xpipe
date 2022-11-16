package io.xpipe.core.store;

public interface CommandsStore extends DataStore {

    CommandProcessControl create() throws Exception;
}
