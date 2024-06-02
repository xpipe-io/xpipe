package io.xpipe.core.store;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public interface NetworkTunnelStore extends DataStore {

    static AtomicInteger portCounter = new AtomicInteger();

    public static int randomPort() {
        var p = 40000 + portCounter.get();
        portCounter.set(portCounter.get() + 1 % 1000);
        return p;
    }

    interface TunnelFunction {

        NetworkTunnelSession create(int localPort, int remotePort);
    }

    DataStore getNetworkParent();

    default boolean requiresTunnel() {
        NetworkTunnelStore current = this;
        while (true) {
            var func = current.tunnelSession();
            if (func != null) {
                return true;
            }

            if (current.getNetworkParent() == null) {
                return false;
            }

            if (current.getNetworkParent() instanceof NetworkTunnelStore t) {
                current = t;
            } else {
                return false;
            }
        }
    }

    default boolean isLocallyTunneable() {
        NetworkTunnelStore current = this;
        while (true) {
            if (current.getNetworkParent() == null) {
                return true;
            }

            if (current.getNetworkParent() instanceof NetworkTunnelStore t) {
                current = t;
            } else {
                return false;
            }
        }
    }

    default NetworkTunnelSession sessionChain(int local, int remotePort) throws Exception {
        if (!isLocallyTunneable()) {
            throw new IllegalStateException();
        }

        var running = new AtomicBoolean();
        var runningCounter = new AtomicInteger();
        var counter = new AtomicInteger();
        var sessions = new ArrayList<NetworkTunnelSession>();
        NetworkTunnelStore current = this;
        do {
            var func = current.tunnelSession();
            if (func == null) {
                continue;
            }

            var currentLocalPort = isLast(current) ? local : randomPort();
            var currentRemotePort = sessions.isEmpty() ? remotePort : sessions.getLast().getLocalPort();
            var t = func.create(currentLocalPort, currentRemotePort);
            t.addListener(r -> {
                if (r) {
                    runningCounter.incrementAndGet();
                } else {
                    runningCounter.decrementAndGet();
                }
                running.set(runningCounter.get() == counter.get());
            });
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
                public void start() throws Exception {

                }

                @Override
                public void stop() throws Exception {

                }

                @Override
                public int getLocalPort() {
                    return local;
                }

                @Override
                public int getRemotePort() {
                    return remotePort;
                }
            };
        }

        return new SessionChain(running1 -> {}, sessions);
    }

    default boolean isLast(NetworkTunnelStore tunnelStore) {
        NetworkTunnelStore current = tunnelStore;
        while ((current = (NetworkTunnelStore) current.getNetworkParent()) != null) {
            var func = current.tunnelSession();
            if (func != null) {
                return false;
            }
        }
        return true;
    }

    default TunnelFunction tunnelSession() {
        return null;
    }
}
