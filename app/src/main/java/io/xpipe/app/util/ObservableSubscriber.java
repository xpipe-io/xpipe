package io.xpipe.app.util;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ObservableSubscriber implements Observable {

    private final IntegerProperty property = new SimpleIntegerProperty();

    public void trigger() {
        property.set(property.get() + 1);
        property.getValue();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        property.addListener(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        property.removeListener(listener);
    }
}
