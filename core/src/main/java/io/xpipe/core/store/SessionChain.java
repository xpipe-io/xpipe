package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import java.util.List;

public class SessionChain extends NetworkTunnelSession {

    private final List<NetworkTunnelSession> sessions;

    public SessionChain(SessionListener listener, List<NetworkTunnelSession> sessions) {
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
        for (Session session : sessions) {
            session.start();
        }
    }

    @Override
    public void stop() throws Exception {
        for (Session session : sessions) {
            session.stop();
        }
    }
}
