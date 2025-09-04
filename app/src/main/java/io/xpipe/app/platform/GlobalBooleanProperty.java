package io.xpipe.app.platform;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

public class GlobalBooleanProperty extends SimpleBooleanProperty {

    public GlobalBooleanProperty() {}

    public GlobalBooleanProperty(boolean initialValue) {
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
    public synchronized void addListener(ChangeListener<? super Boolean> listener) {
        super.addListener(listener);
    }

    @Override
    public synchronized void removeListener(ChangeListener<? super Boolean> listener) {
        super.removeListener(listener);
    }
}
