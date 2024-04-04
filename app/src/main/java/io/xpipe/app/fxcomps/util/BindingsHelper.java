package io.xpipe.app.fxcomps.util;

import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import lombok.Value;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@SuppressWarnings("InfiniteLoopStatement")
public class BindingsHelper {

    private static final Set<ReferenceEntry> REFERENCES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    static {
        ThreadHelper.createPlatformThread("referenceGC", true, () -> {
                    while (true) {
                        for (ReferenceEntry reference : REFERENCES) {
                            if (reference.canGc()) {
                                REFERENCES.remove(reference);
                            }
                        }
                        ThreadHelper.sleep(1000);

                        // Use for testing
                        // System.gc();
                    }
                })
                .start();
    }

    public static void preserve(Object source, Object target) {
        REFERENCES.add(new ReferenceEntry(new WeakReference<>(source), target));
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
