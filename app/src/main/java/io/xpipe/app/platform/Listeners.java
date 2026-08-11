package io.xpipe.app.platform;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Listeners {

    public static <V, T> void listenWeak(V ref, ObservableValue<T> value, BiConsumer<V, T> consumer) {
        var weakRef = new WeakReference<>(ref);
        var listener = new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                var v = weakRef.get();
                if (v != null) {
                    consumer.accept(v, newValue);
                }
            }
        };
        value.addListener(listener);
    }

    public static <V, T> void subscribeWeak(V ref, ObservableValue<T> value, BiConsumer<V, T> consumer) {
        listenWeak(ref, value, consumer);
        consumer.accept(ref, value.getValue());
    }

    public static <T> void subscribeList(ObservableList<T> l, Runnable r) {
        subscribeList(l, ignored -> r.run());
    }

    @SuppressWarnings("unchecked")
    public static <T> void subscribeList(ObservableList<T> l, Consumer<List<T>> r) {
        r.accept(l);
        l.addListener((ListChangeListener<? super T>) c -> {
            r.accept((List<T>) c.getList());
        });
    }

    public static <R extends Region, T> void attachWithScene(R node, ObservableValue<T> value, Consumer<T> consumer) {
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

    public static <T> void listen(ObservableValue<Boolean> enabled, ObservableList<T> value, Consumer<ListChangeListener.Change<? extends T>> consumer) {
        var listener = new ListChangeListener<T>() {
            @Override
            public void onChanged(Change<? extends T> c) {
                consumer.accept(c);
            }
        };
        enabled.subscribe(v -> {
            if (v) {
                value.addListener(listener);
            } else {
                value.removeListener(listener);
            }
        });
    }
}
