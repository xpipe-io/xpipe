package io.xpipe.app.util;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;

public class GlobalStringProperty extends SimpleStringProperty {

    public GlobalStringProperty() {
    }

    public GlobalStringProperty(String initialValue) {
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
    public synchronized void addListener(ChangeListener<? super String> listener) {
        super.addListener(listener);
    }

    @Override
    public synchronized void removeListener(ChangeListener<? super String> listener) {
        super.removeListener(listener);
    }
}
