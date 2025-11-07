package io.xpipe.app.platform;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public class GlobalObjectProperty<T> extends SimpleObjectProperty<T> {

    public GlobalObjectProperty() {}

    public GlobalObjectProperty(T initialValue) {
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
    public synchronized void addListener(ChangeListener<? super T> listener) {
        super.addListener(listener);
    }

    @Override
    public synchronized void removeListener(ChangeListener<? super T> listener) {
        super.removeListener(listener);
    }
}
