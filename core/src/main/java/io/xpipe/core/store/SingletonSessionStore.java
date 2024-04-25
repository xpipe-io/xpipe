package io.xpipe.core.store;

public interface SingletonSessionStore<T extends SingletonSessionStore.Session>
        extends ExpandedLifecycleStore, InternalCacheDataStore {

    abstract class Session {

        public abstract boolean isRunning();

        public abstract void start() throws Exception;

        public abstract void stop() throws Exception;
    }

    @Override
    default void finalizeValidate() throws Exception {
        stopSessionIfNeeded();
    }

    default void setSessionEnabled(boolean value) {
        setCache("sessionEnabled", value);
    }

    default boolean isSessionRunning() {
        return getCache("sessionRunning", Boolean.class, false);
    }

    default boolean isSessionEnabled() {
        return getCache("sessionEnabled", Boolean.class, false);
    }

    default void onSessionUpdate(boolean active) {
        setSessionEnabled(active);
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
            setSessionEnabled(true);
            if (s != null) {
                if (s.isRunning()) {
                    return;
                }

                s.start();
                return;
            }

            try {
                s = newSession();
                s.start();
                setCache("session", s);
                onSessionUpdate(true);
            } catch (Exception ex) {
                onSessionUpdate(false);
                throw ex;
            }
        }
    }

    default void stopSessionIfNeeded() throws Exception {
        synchronized (this) {
            var ex = getSession();
            setSessionEnabled(false);
            if (ex != null) {
                ex.stop();
                setCache("session", null);
                onSessionUpdate(false);
            }
        }
    }
}
