package io.xpipe.app.platform;

import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

import lombok.Value;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings("InfiniteLoopStatement")
public class BindingsHelper {

    private static final Set<ReferenceEntry> REFERENCES = new HashSet<>();

    static {
        ThreadHelper.createPlatformThread("Binding Reference GC", true, () -> {
                    while (true) {
                        synchronized (REFERENCES) {
                            REFERENCES.removeIf(ReferenceEntry::canGc);
                        }
                        ThreadHelper.sleep(1000);

                        // Use for testing
                        // System.gc();
                    }
                })
                .start();
    }

    public static void preserve(Object source, Object target) {
        synchronized (REFERENCES) {
            REFERENCES.add(new ReferenceEntry(new WeakReference<>(source), target));
        }
    }

    public static <T, U> ObjectBinding<U> map(
            ObservableValue<T> observableValue, Function<? super T, ? extends U> mapper) {
        return Bindings.createObjectBinding(
                () -> {
                    return mapper.apply(observableValue.getValue());
                },
                observableValue);
    }

    public static <T> BooleanBinding mapBoolean(
            ObservableValue<T> observableValue, Function<? super T, Boolean> mapper) {
        return Bindings.createBooleanBinding(
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
