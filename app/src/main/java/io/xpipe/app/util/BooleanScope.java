package io.xpipe.app.util;

import io.xpipe.core.util.FailableRunnable;
import javafx.beans.property.BooleanProperty;

public class BooleanScope implements AutoCloseable {

    private final BooleanProperty prop;
    private boolean wait;

    public BooleanScope(BooleanProperty prop) {
        this.prop = prop;
    }

    public static <E extends Throwable> void executeExclusive(BooleanProperty prop, FailableRunnable<E> r) throws E {
        try (var ignored = new BooleanScope(prop).exclusive().start()) {
            r.run();
        }
    }

    public BooleanScope exclusive() {
        this.wait = true;
        return this;
    }

    public synchronized BooleanScope start() {
        if (wait) {
            while (prop.get()) {
                ThreadHelper.sleep(50);
            }
        }
        prop.setValue(true);

        return this;
    }

    @Override
    public synchronized void close() {
        prop.setValue(false);
    }
}
