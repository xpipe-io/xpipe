package io.xpipe.core.store;

public abstract class Session implements AutoCloseable {

    protected SessionListener listener;

    protected Session(SessionListener listener) {
        this.listener = listener;
    }

    public void addListener(SessionListener n) {
        var current = this.listener;
        this.listener = running -> {
            current.onStateChange(running);
            n.onStateChange(running);
        };
    }

    public abstract boolean isRunning();

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;

    @Override
    public void close() throws Exception {
        stop();
    }
}
