package io.xpipe.app.util;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.function.Function;

@SuppressWarnings("InfiniteLoopStatement")
public class BindingsHelper {

    private static final WeakHashMap<Object, Object> REFERENCES = new WeakHashMap<>();

    public static void preserve(Object source, Object target) {
        synchronized (REFERENCES) {
            REFERENCES.put(source, target);
        }
    }

    public static <T, U> ObservableValue<U> map(
            ObservableValue<T> observableValue, Function<? super T, ? extends U> mapper) {
        return Bindings.createObjectBinding(
                () -> {
                    return mapper.apply(observableValue.getValue());
                },
                observableValue);
    }

    public static <T, U> ObservableValue<U> flatMap(
            ObservableValue<T> observableValue, Function<? super T, ? extends ObservableValue<? extends U>> mapper) {
        var prop = new SimpleObjectProperty<U>();
        Runnable runnable = () -> {
            prop.bind(mapper.apply(observableValue.getValue()));
        };
        runnable.run();
        observableValue.addListener((observable, oldValue, newValue) -> {
            runnable.run();
        });
        preserve(prop, observableValue);
        return prop;
    }

    @Value
    private static class ReferenceEntry {

        WeakReference<?> source;
        Object target;

        public boolean canGc() {
            return source.get() == null;
        }
    }
}
