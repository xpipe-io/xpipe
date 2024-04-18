package io.xpipe.app.util;

import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.core.util.FailableRunnable;

import javafx.beans.property.BooleanProperty;

public class BooleanScope implements AutoCloseable {

    private final BooleanProperty prop;
    private boolean invert;
    private boolean forcePlatform;
    private boolean wait;

    public BooleanScope(BooleanProperty prop) {
        this.prop = prop;
    }

    public static <E extends Throwable> void execute(BooleanProperty prop, FailableRunnable<E> r) throws E {
        try (var ignored = new BooleanScope(prop).start()) {
            r.run();
        }
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

    public BooleanScope invert() {
        this.invert = true;
        return this;
    }

    public BooleanScope forcePlatform() {
        this.forcePlatform = true;
        return this;
    }

    public BooleanScope start() {
        if (wait) {
            while (!invert == prop.get()) {
                ThreadHelper.sleep(50);
            }
        }
        if (forcePlatform) {
            PlatformThread.runLaterIfNeeded(() -> prop.setValue(!invert));
        } else {
            prop.setValue(!invert);
        }

        return this;
    }

    @Override
    public void close() {
        if (forcePlatform) {
            PlatformThread.runLaterIfNeeded(() -> prop.setValue(invert));
        } else {
            prop.setValue(invert);
        }
    }
}
