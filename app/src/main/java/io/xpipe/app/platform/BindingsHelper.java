package io.xpipe.app.platform;

import io.xpipe.app.util.ThreadHelper;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

import lombok.Value;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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

    public static <T> void subscribeList(
            ObservableList<T> l, Runnable r) {
        r.run();
        l.addListener((ListChangeListener<? super T>) c -> {
            r.run();
        });
    }

    public static <T, U> ObservableValue<U> flatMap(
            ObservableValue<T> observableValue, Function<? super T, ? extends ObservableValue<? extends U>> mapper) {
        var prop = new SimpleObjectProperty<U>();
        Runnable runnable = () -> {
            var observable = mapper.apply(observableValue.getValue());
            if (observable != null) {
                prop.bind(observable);
            } else {
                prop.unbind();
                prop.setValue(null);
            }
        };
        runnable.run();
        observableValue.addListener((observable, oldValue, newValue) -> {
            runnable.run();
        });
        preserve(prop, observableValue);
        return prop;
    }

    public static <R extends Region, T> void attach(R node, ObservableValue<T> value, Consumer<T> consumer) {
        var listener = new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                consumer.accept(newValue);
            }
        };
        node.sceneProperty().subscribe(scene -> {
            if (scene != null) {
                consumer.accept(value.getValue());
                value.addListener(listener);
            } else {
                value.removeListener(listener);
            }
        });
    }

    public static void attach(ObservableValue<Boolean> enabled, ObservableValue<?> value, Runnable r) {
        attach(enabled, value, (ignored) -> r.run());
    }

    public static <T> void attach(ObservableValue<Boolean> enabled, ObservableValue<T> value, Consumer<T> consumer) {
        var listener = new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                consumer.accept(newValue);
            }
        };
        enabled.subscribe(v -> {
            if (v) {
                consumer.accept(value.getValue());
                value.addListener(listener);
            } else {
                value.removeListener(listener);
            }
        });
    }

    public static <T> void attach(ObservableValue<Boolean> enabled, ObservableList<T> value, Runnable consumer) {
        var listener = new ListChangeListener<T>() {
            @Override
            public void onChanged(Change<? extends T> c) {
                consumer.run();
            }
        };
        enabled.subscribe(v -> {
            if (v) {
                consumer.run();
                value.addListener(listener);
            } else {
                value.removeListener(listener);
            }
        });
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
