package io.xpipe.core.store;

public interface SingletonToggleSessionStore<T extends SingletonSessionStore.Session> extends SingletonSessionStore<T>, StatefulDataStore<ToggleSessionState>, ValidatableStore {

    @Override
    default Class<ToggleSessionState> getStateClass() {
        return ToggleSessionState.class;
    }

    @Override
    public default void onSessionUpdate(boolean active) {
        var c = getState();
        c.setRunning(active);
        if (!active) {
            c.setEnabled(false);
        } else {
            c.setEnabled(true);
        }
        setState(c);
    }

    @Override
    public default void validate() throws Exception {
        if (getState().getEnabled() != null) {
            if (getState().getEnabled()) {
                startIfNeeded();
            } else {
                stopIfNeeded();
            }
        }
    }

}
