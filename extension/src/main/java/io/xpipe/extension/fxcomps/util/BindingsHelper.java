package io.xpipe.extension.fxcomps.util;

import javafx.beans.binding.Binding;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BindingsHelper {

    /*
    TODO: Proper cleanup. Maybe with a separate thread?
     */
    private static final Map<WeakReference<Object>, Set<ObservableValue<?>>> BINDINGS = new ConcurrentHashMap<>();

    public static <T extends Binding<?>> T persist(T binding) {
        var dependencies = new HashSet<ObservableValue<?>>();
        while (dependencies.addAll(binding.getDependencies().stream()
                .map(o -> (ObservableValue<?>) o)
                .toList())) {}
        dependencies.add(binding);
        BINDINGS.put(new WeakReference<>(binding), dependencies);
        return binding;
    }

    public static <T> void bindContent(ObservableList<T> l1, ObservableList<? extends T> l2) {
        setContent(l1, l2);
        l2.addListener((ListChangeListener<? super T>) c -> {
            setContent(l1, l2);
        });
    }

    public static <T, V> void bindMappedContent(ObservableList<T> l1, ObservableList<V> l2, Function<V, T> map) {
        Runnable runnable = () -> {
            setContent(l1, l2.stream().map(map).toList());
        };
        runnable.run();
        l2.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
    }

    public static <T> void setContent(ObservableList<T> toSet, List<? extends T> newList) {
        if (toSet.equals(newList)) {
            return;
        }

        if (toSet.size() == 0) {
            toSet.setAll(newList);
            return;
        }

        if (newList.containsAll(toSet)) {
            var l = new ArrayList<>(newList);
            l.removeIf(t -> !toSet.contains(t));
            if (!l.equals(toSet)) {
                toSet.setAll(newList);
                return;
            }

            var start = 0;
            for (int end = 0; end <= toSet.size(); end++) {
                var index = end < toSet.size() ? newList.indexOf(toSet.get(end)) : newList.size();
                for (; start < index; start++) {
                    toSet.add(start, newList.get(start));
                }
                start = index + 1;
            }
            return;
        }

        if (toSet.contains(newList)) {
            var l = new ArrayList<>(newList);
            l.removeAll(toSet);
            newList.removeAll(l);
            return;
        }

        toSet.removeIf(e -> !newList.contains(e));

        if (toSet.size() + 1 == newList.size() && newList.containsAll(toSet)) {
            var l = new ArrayList<>(newList);
            l.removeAll(toSet);
            var index = newList.indexOf(l.get(0));
            toSet.add(index, l.get(0));
            return;
        }

        if (toSet.size() - 1 == newList.size() && toSet.containsAll(newList)) {
            var l = new ArrayList<>(toSet);
            l.removeAll(newList);
            toSet.remove(l.get(0));
            return;
        }

        toSet.setAll(newList);
    }
}
