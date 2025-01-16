package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import java.util.List;

public class NetworkTunnelSessionChain extends NetworkTunnelSession {

    private final List<NetworkTunnelSession> sessions;

    public NetworkTunnelSessionChain(SessionListener listener, List<NetworkTunnelSession> sessions) {
        super(listener);
        this.sessions = sessions;
    }

    public ShellControl getShellControl() {
        return sessions.getLast().getShellControl();
    }

    public int getLocalPort() {
        return sessions.getFirst().getLocalPort();
    }

    @Override
    public int getRemotePort() {
        return sessions.getLast().getRemotePort();
    }

    @Override
    public boolean isRunning() {
        return sessions.stream().allMatch(session -> session.isRunning());
    }

    @Override
    public void start() throws Exception {
        for (var i = 0; i < sessions.size(); i++) {
            try {
                sessions.get(i).start();
            } catch (Exception e) {
                for (var j = 0; j < i; j++) {
                    var started = sessions.get(j);
                    try {
                        started.stop();
                    } catch (Exception stopEx) {
                        e.addSuppressed(stopEx);
                    }
                }
                throw e;
            }
        }
    }

    @Override
    public void stop() throws Exception {
        Exception ex = null;
        for (var i = sessions.size() - 1; i >= 0; i--) {
            try {
                sessions.get(i).stop();
            } catch (Exception e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }
}
