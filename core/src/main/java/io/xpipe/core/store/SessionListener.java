package io.xpipe.core.store;

public interface SessionListener {

    void onStateChange(boolean running);
}
