package io.xpipe.extension.util;

import javafx.beans.property.BooleanProperty;

public class BusyProperty implements AutoCloseable {

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
