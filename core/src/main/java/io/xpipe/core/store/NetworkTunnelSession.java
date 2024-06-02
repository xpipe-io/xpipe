package io.xpipe.core.store;

public abstract class NetworkTunnelSession extends Session {

    protected NetworkTunnelSession(SessionListener listener) {
        super(listener);
    }

    public abstract int getLocalPort();

    public abstract int getRemotePort();
}
