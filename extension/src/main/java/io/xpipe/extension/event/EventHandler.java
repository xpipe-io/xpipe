package io.xpipe.extension.event;

import java.util.List;
import java.util.ServiceLoader;

public abstract class EventHandler {

    private static EventHandler INSTANCE;

    private static void init() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(EventHandler.class).findFirst().orElseThrow();
        }
    }

    public static EventHandler get() {
        init();
        return INSTANCE;
    }

    public abstract List<TrackEvent> snapshotEvents();

    public abstract void handle(TrackEvent te);

    public abstract void handle(ErrorEvent ee);
}
