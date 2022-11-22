package io.xpipe.extension.event;

import java.util.List;
import java.util.ServiceLoader;

public abstract class EventHandler {

    public static final EventHandler DEFAULT = new EventHandler() {
        @Override
        public List<TrackEvent> snapshotEvents() {
            return List.of();
        }

        @Override
        public void handle(TrackEvent te) {
            var cat = te.getCategory();
            if (cat == null) {
                cat = "log";
            }
            System.out.println("[" + cat + "] " + te.toString());
            System.out.flush();
        }

        @Override
        public void handle(ErrorEvent ee) {
            if (ee.getDescription() != null) System.err.println(ee.getDescription());
            if (ee.getThrowable() != null) ee.getThrowable().printStackTrace();
        }
    };

    private static EventHandler INSTANCE;

    private static void init() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(EventHandler.class).findFirst().orElse(DEFAULT);
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
