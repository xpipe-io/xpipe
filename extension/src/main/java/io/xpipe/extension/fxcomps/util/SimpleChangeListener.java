package io.xpipe.extension.fxcomps.util;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

@FunctionalInterface
public interface SimpleChangeListener<T> {

    static <T> void apply(ObservableValue<T> obs, SimpleChangeListener<T> cl) {
        obs.addListener(cl.wrapped());
        cl.onChange(obs.getValue());
    }

    void onChange(T val);

    default ChangeListener<T> wrapped() {
        return (observable, oldValue, newValue) -> this.onChange(newValue);
    }
}
