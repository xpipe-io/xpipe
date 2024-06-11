package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

public abstract class NetworkTunnelSession extends Session {

    protected NetworkTunnelSession(SessionListener listener) {
        super(listener);
    }

    public abstract int getLocalPort();

    public abstract int getRemotePort();

    public abstract ShellControl getShellControl();
}
