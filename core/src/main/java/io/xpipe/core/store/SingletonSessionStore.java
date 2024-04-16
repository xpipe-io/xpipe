package io.xpipe.core.store;

public interface SingletonSessionStore<T extends SingletonSessionStore.Session> extends InternalCacheDataStore {

    static abstract class Session {

        public abstract boolean isRunning();

        public abstract void start() throws Exception;

        public abstract void stop() throws Exception;
    }

    void onSessionUpdate(boolean active);

    T newSession() throws Exception;

    Class<?> getSessionClass();

    @SuppressWarnings("unchecked")
    default T getSession() {
        return (T) getCache("session", getSessionClass(), null);
    }

    default void startIfNeeded() throws Exception {
        var s = getSession();
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


    default void stopIfNeeded() throws Exception {
        var ex = getSession();
        setCache("session", null);
        if (ex != null) {
            ex.stop();
            onSessionUpdate(false);
        }
    }
}
