package io.xpipe.core.store;

public abstract class Session {

    public abstract boolean isRunning();

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;
}
