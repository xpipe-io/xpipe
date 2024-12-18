package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public interface NetworkTunnelStore extends DataStore {

    AtomicInteger portCounter = new AtomicInteger();

    static int randomPort() {
        var p = 40000 + portCounter.get();
        portCounter.set(portCounter.get() + 1 % 1000);
        return p;
    }

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

    default NetworkTunnelSession sessionChain(int local, int remotePort, String address) throws Exception {
        if (!isLocallyTunnelable()) {
            throw new IllegalStateException(
                    "Unable to create tunnel chain as one intermediate system does not support tunneling");
        }

        if (getNetworkParent() == null) {
            return null;
        }

        var counter = new AtomicInteger();
        var sessions = new ArrayList<NetworkTunnelSession>();
        NetworkTunnelStore current = this;
        do {
            var currentLocalPort = current.isLast() ? local : randomPort();
            var currentRemotePort =
                    sessions.isEmpty() ? remotePort : sessions.getLast().getLocalPort();
            var t = current.createTunnelSession(
                    currentLocalPort, currentRemotePort, current == this ? address : "localhost");
            t.start();
            sessions.add(t);
            counter.incrementAndGet();
        } while ((current = (NetworkTunnelStore) current.getNetworkParent()) != null);

        if (sessions.size() == 1) {
            return sessions.getFirst();
        }

        if (sessions.isEmpty()) {
            return new NetworkTunnelSession(null) {

                @Override
                public boolean isRunning() {
                    return false;
                }

                @Override
                public void start() {}

                @Override
                public void stop() {}

                @Override
                public int getLocalPort() {
                    return remotePort;
                }

                @Override
                public int getRemotePort() {
                    return remotePort;
                }

                @Override
                public ShellControl getShellControl() {
                    return null;
                }
            };
        }

        return new SessionChain(running1 -> {}, sessions);
    }

    default boolean isLast() {
        return getNetworkParent() != null
                && getNetworkParent() instanceof NetworkTunnelStore n
                && n.getNetworkParent() == null;
    }

    NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address) throws Exception;
}
