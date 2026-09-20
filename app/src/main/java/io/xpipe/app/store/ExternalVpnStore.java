package io.xpipe.app.store;

public interface ExternalVpnStore extends DataStore, SelfReferentialStore, SingletonSessionStore<ExternalVpnStoreSession> {

    @Override
    default ExternalVpnStoreSession newSession() {
        return new ExternalVpnStoreSession(getSelfEntry().ref());
    }

    @Override
    default Class<?> getSessionClass() {
        return ExternalVpnStoreSession.class;
    }

    void startImpl() throws Exception;

    void stopImpl() throws Exception;

    boolean checkRunning() throws Exception;
}
