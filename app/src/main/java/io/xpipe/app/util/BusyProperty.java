package io.xpipe.app.util;

import javafx.beans.property.BooleanProperty;
import org.apache.commons.lang3.function.FailableRunnable;

public class BusyProperty implements AutoCloseable {

    public static <E extends Throwable> void execute(BooleanProperty prop, FailableRunnable<E> r) throws E {
        try (var ignored = new BusyProperty(prop)) {
            r.run();
        }
    }

    private final BooleanProperty prop;

    public BusyProperty(BooleanProperty prop) {
        this.prop = prop;
        prop.setValue(true);
    }

    @Override
    public void close() {
        prop.setValue(false);
    }
}
