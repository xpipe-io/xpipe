package io.xpipe.app.util;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import lombok.Getter;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public class DerivedObservableList<T> {

    private final ObservableList<T> list;
    private final boolean unique;

    public DerivedObservableList(ObservableList<T> list, boolean unique) {
        this.list = list;
        this.unique = unique;
    }

    private <V> DerivedObservableList<V> createNewDerived() {
        var l = FXCollections.<V>observableArrayList();
        BindingsHelper.preserve(l, list);
        return new DerivedObservableList<>(l, unique);
    }

    public void setContent(List<? extends T> newList) {
        if (list.equals(newList)) {
            return;
        }

        if (list.size() == 0) {
            list.addAll(newList);
            return;
        }

        if (newList.size() == 0) {
            list.clear();
            return;
        }

        if (unique) {
            setContentUnique(newList);
        } else {
            setContentNonUnique(newList);
        }
    }

    private void setContentNonUnique(List<? extends T> newList) {
        var target = list;
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

    private void setContentUnique(List<? extends T> newList) {
        var listSet = new HashSet<>(list);
        var newSet = new HashSet<>(newList);
        // Addition
        if (newSet.containsAll(list)) {
            var l = new ArrayList<>(newList);
            l.removeIf(t -> !listSet.contains(t));
            // Reordering occurred
            if (!l.equals(list)) {
                list.setAll(newList);
                return;
            }

            var start = 0;
            for (int end = 0; end <= list.size(); end++) {
                var index = end < list.size() ? newList.indexOf(list.get(end)) : newList.size();
                for (; start < index; start++) {
                    list.add(start, newList.get(start));
                }
                start = index + 1;
            }
            return;
        }

        // Removal
        if (listSet.containsAll(newList)) {
            var l = new ArrayList<>(list);
            l.removeIf(t -> !newSet.contains(t));
            // Reordering occurred
            if (!l.equals(newList)) {
                list.setAll(newList);
                return;
            }

            var toRemove = new ArrayList<>(list);
            toRemove.removeIf(t -> newSet.contains(t));
            list.removeAll(toRemove);
            return;
        }

        // Other cases are more difficult
        list.setAll(newList);
    }

    public <V> DerivedObservableList<V> mapped(Function<T, V> map) {
        var cache = new HashMap<T, V>();
        var l1 = this.<V>createNewDerived();
        Runnable runnable = () -> {
            cache.keySet().removeIf(t -> !getList().contains(t));
            l1.setContent(list.stream()
                    .map(v -> {
                        if (!cache.containsKey(v)) {
                            cache.put(v, map.apply(v));
                        }

                        return cache.get(v);
                    })
                    .toList());
        };
        runnable.run();
        list.addListener((ListChangeListener<? super T>) c -> {
            runnable.run();
        });
        return l1;
    }

    public void bindContent(ObservableList<T> other) {
        setContent(other);
        other.addListener((ListChangeListener<? super T>) c -> {
            setContent(other);
        });
    }

    public DerivedObservableList<T> filtered(Predicate<T> predicate) {
        return filtered(new SimpleObjectProperty<>(predicate));
    }

    public DerivedObservableList<T> filtered(Predicate<T> predicate, Observable... observables) {
        return filtered(Bindings.createObjectBinding(
                () -> {
                    return new Predicate<>() {
                        @Override
                        public boolean test(T v) {
                            return predicate.test(v);
                        }
                    };
                },
                Arrays.stream(observables).filter(Objects::nonNull).toArray(Observable[]::new)));
    }

    public DerivedObservableList<T> filtered(ObservableValue<Predicate<T>> predicate) {
        var d = this.<T>createNewDerived();
        Runnable runnable = () -> {
            d.setContent(
                    predicate.getValue() != null
                            ? list.stream().filter(predicate.getValue()).toList()
                            : list);
        };
        runnable.run();
        list.addListener((ListChangeListener<? super T>) c -> {
            runnable.run();
        });
        predicate.addListener(observable -> {
            runnable.run();
        });
        return d;
    }

    public DerivedObservableList<T> sorted(Comparator<T> comp, Observable... observables) {
        return sorted(Bindings.createObjectBinding(
                () -> {
                    return new Comparator<>() {
                        @Override
                        public int compare(T o1, T o2) {
                            return comp.compare(o1, o2);
                        }
                    };
                },
                observables));
    }

    public DerivedObservableList<T> sorted(ObservableValue<Comparator<T>> comp) {
        var d = this.<T>createNewDerived();
        Runnable runnable = () -> {
            d.setContent(list.stream().sorted(comp.getValue()).toList());
        };
        runnable.run();
        list.addListener((ListChangeListener<? super T>) c -> {
            runnable.run();
        });
        comp.addListener(observable -> {
            d.list.sort(comp.getValue());
        });
        return d;
    }

    public DerivedObservableList<T> blockUpdatesIf(ObservableBooleanValue block) {
        var d = this.<T>createNewDerived();
        Runnable runnable = () -> {
            d.setContent(list);
        };
        runnable.run();
        list.addListener((ListChangeListener<? super T>) c -> {
            runnable.run();
        });
        block.addListener(observable -> {
            runnable.run();
        });
        return d;
    }
}
