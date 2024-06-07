package io.xpipe.app.fxcomps.util;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ListBindingsHelper {

    public static <T> void bindContent(ObservableList<T> l1, ObservableList<? extends T> l2) {
        setContent(l1, l2);
        l2.addListener((ListChangeListener<? super T>) c -> {
            setContent(l1, l2);
        });
    }

    public static <T, U> ObservableValue<Boolean> anyMatch(List<? extends ObservableValue<Boolean>> l) {
        return Bindings.createBooleanBinding(
                () -> {
                    return l.stream().anyMatch(booleanObservableValue -> booleanObservableValue.getValue());
                },
                l.toArray(ObservableValue[]::new));
    }

    public static <T, V> ObservableList<T> mappedContentBinding(ObservableList<V> l2, Function<V, T> map) {
        ObservableList<T> l1 = FXCollections.observableList(new ArrayList<>());
        Runnable runnable = () -> {
            setContent(l1, l2.stream().map(map).toList());
        };
        runnable.run();
        l2.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
        BindingsHelper.preserve(l1, l2);
        return l1;
    }

    public static <T, V> ObservableList<T> cachedMappedContentBinding(
            ObservableList<V> all, ObservableList<V> shown, Function<V, T> map) {
        var cache = new HashMap<V, T>();

        ObservableList<T> l1 = FXCollections.observableList(new ArrayList<>());
        Runnable runnable = () -> {
            cache.keySet().removeIf(t -> !all.contains(t));
            setContent(
                    l1,
                    shown.stream()
                            .map(v -> {
                                if (!cache.containsKey(v)) {
                                    cache.put(v, map.apply(v));
                                }

                                return cache.get(v);
                            })
                            .toList());
        };
        runnable.run();
        shown.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
        BindingsHelper.preserve(l1, all);
        BindingsHelper.preserve(l1, shown);
        return l1;
    }

    public static <V> ObservableList<V> orderedContentBinding(
            ObservableList<V> l2, Comparator<V> comp, Observable... observables) {
        return orderedContentBinding(
                l2,
                Bindings.createObjectBinding(
                        () -> {
                            return new Comparator<>() {
                                @Override
                                public int compare(V o1, V o2) {
                                    return comp.compare(o1, o2);
                                }
                            };
                        },
                        observables));
    }

    public static <V> ObservableList<V> orderedContentBinding(
            ObservableList<V> l2, ObservableValue<Comparator<V>> comp) {
        ObservableList<V> l1 = FXCollections.observableList(new ArrayList<>());
        Runnable runnable = () -> {
            setContent(l1, l2.stream().sorted(comp.getValue()).toList());
        };
        runnable.run();
        l2.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
        comp.addListener((observable, oldValue, newValue) -> {
            runnable.run();
        });
        BindingsHelper.preserve(l1, l2);
        return l1;
    }

    public static <V> ObservableList<V> filteredContentBinding(ObservableList<V> l2, Predicate<V> predicate) {
        return filteredContentBinding(l2, new SimpleObjectProperty<>(predicate));
    }

    public static <V> ObservableList<V> filteredContentBinding(
            ObservableList<V> l2, Predicate<V> predicate, Observable... observables) {
        return filteredContentBinding(
                l2,
                Bindings.createObjectBinding(
                        () -> {
                            return new Predicate<>() {
                                @Override
                                public boolean test(V v) {
                                    return predicate.test(v);
                                }
                            };
                        },
                        Arrays.stream(observables).filter(Objects::nonNull).toArray(Observable[]::new)));
    }

    public static <V> ObservableList<V> filteredContentBinding(
            ObservableList<V> l2, ObservableValue<Predicate<V>> predicate) {
        ObservableList<V> l1 = FXCollections.observableList(new ArrayList<>());
        Runnable runnable = () -> {
            setContent(
                    l1,
                    predicate.getValue() != null
                            ? l2.stream().filter(predicate.getValue()).toList()
                            : l2);
        };
        runnable.run();
        l2.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
        predicate.addListener((c, o, n) -> {
            runnable.run();
        });
        BindingsHelper.preserve(l1, l2);
        return l1;
    }

    public static <T> void setContent(ObservableList<T> target, List<? extends T> newList) {
        if (target.equals(newList)) {
            return;
        }

        if (target.size() == 0) {
            target.setAll(newList);
            return;
        }

        if (newList.size() == 0) {
            target.clear();
            return;
        }

        var targetSet = new HashSet<>(target);
        var newSet = new HashSet<>(newList);

        // Only add missing element
        if (target.size() + 1 == newList.size() && newSet.containsAll(targetSet)) {
            var l = new HashSet<>(newSet);
            l.removeAll(targetSet);
            if (l.size() > 0) {
                var found = l.iterator().next();
                var index = newList.indexOf(found);
                target.add(index, found);
                return;
            }
        }

        // Only remove not needed element
        if (target.size() - 1 == newList.size() && targetSet.containsAll(newSet)) {
            var l = new HashSet<>(targetSet);
            l.removeAll(newSet);
            if (l.size() > 0) {
                target.remove(l.iterator().next());
                return;
            }
        }

        // Other cases are more difficult
        target.setAll(newList);
    }
}
