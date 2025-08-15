package io.xpipe.app.ext;

public interface SingletonSessionStore<T extends Session>
        extends ExpandedLifecycleStore, InternalCacheDataStore, SessionListener {

    @Override
    default void finalizeStore() throws Exception {
        stopSessionIfNeeded();
    }

    default boolean isSessionRunning() {
        return getCache("sessionRunning", Boolean.class, false);
    }

    default boolean isSessionEnabled() {
        return getCache("sessionEnabled", Boolean.class, false);
    }

    default void setSessionEnabled(boolean value) {
        setCache("sessionEnabled", value);
    }

    @Override
    default void onStateChange(boolean running) {
        setSessionEnabled(running);
        setCache("sessionRunning", running);
    }

    T newSession() throws Exception;

    Class<?> getSessionClass();

    @SuppressWarnings("unchecked")
    default T getSession() {
        return (T) getCache("session", getSessionClass(), null);
    }

    default T startSessionIfNeeded() throws Exception {
        synchronized (this) {
            var s = getSession();
            if (s != null) {
                if (s.isRunning()) {
                    return s;
                }

                s.start();
                return s;
            }

            try {
                setSessionEnabled(true);
                s = newSession();
                if (s != null) {
                    s.start();
                    setCache("session", s);
                    onStateChange(true);
                    s.addListener(running -> {
                        onStateChange(running);
                    });
                    return s;
                } else {
                    setSessionEnabled(false);
                    return null;
                }
            } catch (Exception ex) {
                setSessionEnabled(false);
                onStateChange(false);
                throw ex;
            }
        }
    }

    default void stopSessionIfNeeded() throws Exception {
        synchronized (this) {
            var ex = getSession();
            setSessionEnabled(false);
            if (ex != null) {
                try {
                    ex.stop();
                } finally {
                    onStateChange(false);
                    setCache("session", null);
                }
            }
        }
    }
}
