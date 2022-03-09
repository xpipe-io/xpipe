package io.xpipe.extension.event;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ServiceLoader;

public abstract class EventHandler {

    private static final EventHandler DEFAULT = new EventHandler() {
        @Override
        public List<TrackEvent> snapshotEvents() {
            return List.of();
        }

        @Override
        public void handle(TrackEvent te) {
            LoggerFactory.getLogger(te.getCategory()).info(te.getMessage());
        }

        @Override
        public void handle(ErrorEvent ee) {
            LoggerFactory.getLogger(EventHandler.class).error(ee.getDescription(), ee.getThrowable());
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
