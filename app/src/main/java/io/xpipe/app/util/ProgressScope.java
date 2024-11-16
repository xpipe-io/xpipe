package io.xpipe.app.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ProgressScope implements AutoCloseable {

    private final DoubleProperty prop;
    private BooleanProperty active = new SimpleBooleanProperty();
    private boolean forcePlatform;

    public ProgressScope(DoubleProperty prop) {
        this.prop = prop;
    }

    public ProgressScope withActive(BooleanProperty active) {
        this.active = active;
        return this;
    }

    public ProgressScope forcePlatform() {
        this.forcePlatform = true;
        return this;
    }

    public ProgressScope start() {
        if (forcePlatform) {
            PlatformThread.runLaterIfNeeded(() -> {
                prop.setValue(0.0);
                active.setValue(true);
            });
        } else {
            prop.setValue(0.0);
            active.setValue(true);
        }

        return this;
    }

    public void set(double val) {
        if (forcePlatform) {
            PlatformThread.runLaterIfNeeded(() -> {
                prop.set(val);
            });
        } else {
            prop.set(val);
        }
    }

    @Override
    public void close() {
        if (forcePlatform) {
            PlatformThread.runLaterIfNeeded(() -> {
                prop.setValue(-1.0);
                active.setValue(false);
            });
        } else {
            prop.setValue(-1.0);
            active.setValue(false);
        }
    }
}
