package io.xpipe.core.store;

public interface SingletonSessionStore<T extends SingletonSessionStore.Session> extends ExpandedLifecycleStore, InternalCacheDataStore {

    static abstract class Session {

        public abstract boolean isRunning();

        public abstract void start() throws Exception;

        public abstract void stop() throws Exception;
    }

    @Override
    public default void finalizeValidate() throws Exception {
        stopSessionIfNeeded();
    }

    default void setEnabled(boolean value) {
        setCache("sessionEnabled", value);
    }

    default boolean isRunning() {
        return getCache("sessionRunning", Boolean.class, false);
    }

    default boolean isEnabled() {
        return getCache("sessionEnabled",Boolean.class,false);
    }

    default void onSessionUpdate(boolean active) {
        setEnabled(active);
        setCache("sessionRunning", active);
    }

    T newSession() throws Exception;

    Class<?> getSessionClass();

    @SuppressWarnings("unchecked")
    default T getSession() {
        return (T) getCache("session", getSessionClass(), null);
    }

    default void startSessionIfNeeded() throws Exception {
        synchronized (this) {
            var s = getSession();
            setEnabled(true);
            if (s != null) {
                if (s.isRunning()) {
                    return;
                }

                s.start();
                return;
            }

            s = newSession();
            s.start();
            setCache("session", s);
            onSessionUpdate(true);
        }
    }


    default void stopSessionIfNeeded() throws Exception {
        synchronized (this) {
            var ex = getSession();
            setEnabled(false);
            if (ex != null) {
                ex.stop();
                setCache("session", null);
                onSessionUpdate(false);
            }
        }
    }
}
