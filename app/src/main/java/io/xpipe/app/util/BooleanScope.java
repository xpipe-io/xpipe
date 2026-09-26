package io.xpipe.app.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class BooleanScope implements AutoCloseable {

    private final BooleanProperty prop;
    private boolean wait;

    public BooleanScope(BooleanProperty prop) {
        this.prop = prop;
    }

    public static BooleanScope noop() {
        return new BooleanScope(new SimpleBooleanProperty());
    }

    public static <E extends Throwable> void executeExclusive(BooleanProperty prop, FailableRunnable<E> r) throws E {
        try (var ignored = new BooleanScope(prop).exclusive().start()) {
            r.run();
        }
    }

    public boolean get() {
        return prop.get();
    }

    public BooleanScope exclusive() {
        this.wait = true;
        return this;
    }

    public BooleanScope start() {
        synchronized (prop) {
            if (wait) {
                while (prop.get()) {
                    ThreadHelper.sleep(50);
                }
            }
            prop.setValue(true);

            return this;
        }
    }

    @Override
    public void close() {
        synchronized (prop) {
            prop.setValue(false);
        }
    }
}
