package io.xpipe.app.issue;

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
            if (ee.getDescription() != null) {
                System.err.println(ee.getDescription());
            }
            if (ee.getThrowable() != null) {
                ee.getThrowable().printStackTrace();
            }
        }

        @Override
        public void modify(ErrorEvent ee) {}
    };

    public static final EventHandler OMIT = new EventHandler() {
        @Override
        public List<TrackEvent> snapshotEvents() {
            return List.of();
        }

        @Override
        public void handle(TrackEvent te) {}

        @Override
        public void handle(ErrorEvent ee) {}

        @Override
        public void modify(ErrorEvent ee) {}
    };
    private static EventHandler INSTANCE;

    public static void set(EventHandler handler) {
        INSTANCE = handler;
    }

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

    public abstract void modify(ErrorEvent ee);
}
