package io.xpipe.app.util;

import io.xpipe.app.comp.store.StoreEntryComp;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import javafx.beans.value.WeakChangeListener;
import lombok.Value;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("InfiniteLoopStatement")
public class BindingsHelper {

    private static final Map<WeakReference<?>, ReferenceEntry> REFERENCES = new HashMap<>();

    static {
        ThreadHelper.createPlatformThread("referenceGC", true, () -> {
                    while (true) {
                        synchronized (REFERENCES) {
                            REFERENCES.values().removeIf(referenceEntry -> {
                                var r = referenceEntry.canGc();
                                if (!r) {
                                    return false;
                                }

                                // System.out.println("Freed " + referenceEntry.getSource().get() + " -> " + referenceEntry.getTarget());
                                return true;
                            });
                        }
                        ThreadHelper.sleep(1000);

                        // Use for testing
                        // System.gc();
                    }
                })
                .start();
    }

    public static WeakReference<?> preserve(Object source, Object target) {
        synchronized (REFERENCES) {
            var ref = new WeakReference<>(source);
            var v = new ReferenceEntry(ref, target);
            REFERENCES.put(ref, v);
            return ref;
        }
    }

    public static <T> WeakChangeListener<T> weak(Object source, ChangeListener<T> listener) {
        preserve(source, source);
        var weak = new WeakChangeListener<>(listener);
        return weak;
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> weak(Object source, Consumer<T> listener) {
        var ref = preserve(source, listener);
        return (T item) -> {
            var v = ref.get();
            if (v == null) {
                return;
            }

            var c = REFERENCES.get(ref);
            if (c == null) {
                return;
            }

            ((Consumer<T>) c.getTarget()).accept(item);
        };
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
