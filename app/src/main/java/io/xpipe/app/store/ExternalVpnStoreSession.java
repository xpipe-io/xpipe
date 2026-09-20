package io.xpipe.app.store;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStoreEntryRef;

public class ExternalVpnStoreSession extends StoreSession {

    private final DataStoreEntryRef<ExternalVpnStore> ref;

    public ExternalVpnStoreSession(DataStoreEntryRef<ExternalVpnStore> ref) {this.ref = ref;}

    @Override
    protected boolean isRunning() {
        return ref.getStore().isSessionRunning();
    }

    @Override
    public void start() throws Exception {
        ref.getStore().startImpl();
        startAliveListener();
    }

    @Override
    public void stop() throws Exception {
        ref.getStore().stopImpl();
    }

    @Override
    protected void handleSessionDeath() {
        try {
            stop();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
        } finally {
            ref.getStore().clearSession();
        }
    }

    @Override
    public boolean checkInactive() {
        return false;
    }

    @Override
    public boolean checkAlive() throws Exception {
        return ref.getStore().checkRunning();
    }
}
