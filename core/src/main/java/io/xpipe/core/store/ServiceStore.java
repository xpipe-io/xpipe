package io.xpipe.core.store;

import java.util.OptionalInt;

public interface ServiceStore extends SingletonSessionStore<SessionChain> {

    NetworkTunnelStore getParent();

    int getPort();

    OptionalInt getTargetPort();

    @Override
    default SessionChain newSession() throws Exception {
        var s = getParent().tunnelSession();
        return null;
    }

    @Override
    default Class<?> getSessionClass() {
        return null;
    }
}
