package io.xpipe.app.util;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;

public class GlobalDoubleProperty extends SimpleDoubleProperty {

    public GlobalDoubleProperty() {}

    public GlobalDoubleProperty(Double initialValue) {
        super(initialValue);
    }

    @Override
    public synchronized void addListener(InvalidationListener listener) {
        super.addListener(listener);
    }

    @Override
    public synchronized void removeListener(InvalidationListener listener) {
        super.removeListener(listener);
    }

    @Override
    public synchronized void addListener(ChangeListener<? super Number> listener) {
        super.addListener(listener);
    }

    @Override
    public synchronized void removeListener(ChangeListener<? super Number> listener) {
        super.removeListener(listener);
    }
}
