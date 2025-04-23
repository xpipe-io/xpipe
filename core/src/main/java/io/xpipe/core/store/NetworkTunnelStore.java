package io.xpipe.core.store;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public interface NetworkTunnelStore extends DataStore {

    DataStore getNetworkParent();

    default boolean requiresTunnel() {
        return getNetworkParent() != null;
    }

    default boolean isLocallyTunnelable() {
        NetworkTunnelStore current = this;
        while (true) {
            var p = current.getNetworkParent();
            if (p == null) {
                return true;
            }

            if (p instanceof NetworkTunnelStore t) {
                current = t;
            } else {
                return false;
            }
        }
    }

    NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address) throws Exception;
}
