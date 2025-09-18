package io.xpipe.ext.base.store;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.HostAddress;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.ext.base.identity.IdentityValue;

public interface HostStore extends DataStore, ShellStore, NetworkTunnelStore {

    HostAddress getHostAddress();

    IdentityValue getIdentity();
}
