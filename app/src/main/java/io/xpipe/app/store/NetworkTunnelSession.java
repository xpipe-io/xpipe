package io.xpipe.app.store;

import io.xpipe.app.process.ShellControl;

public abstract class NetworkTunnelSession extends Session {

    public abstract int getLocalPort();

    public abstract int getRemotePort();

    public abstract ShellControl getShellControl();
}
