package io.xpipe.app.issue;

import java.util.ServiceLoader;

public abstract class EventHandler {

    private static EventHandler INSTANCE;

    public static void set(EventHandler handler) {
        INSTANCE = handler;
    }

    private static void init() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(EventHandler.class).findFirst().orElseThrow();
        }
    }

    public static EventHandler get() {
        init();
        return INSTANCE;
    }

    public abstract void handle(TrackEvent te);

    public abstract void handle(ErrorEvent ee);

    public abstract void modify(ErrorEvent ee);
}
