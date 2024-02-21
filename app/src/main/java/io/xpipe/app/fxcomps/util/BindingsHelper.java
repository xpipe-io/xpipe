package io.xpipe.app.fxcomps.util;

import io.xpipe.app.util.ThreadHelper;
import javafx.beans.Observable;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.Value;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("InfiniteLoopStatement")
public class BindingsHelper {

    private static final Set<ReferenceEntry> REFERENCES = Collections.newSetFromMap(new ConcurrentHashMap<>());
    /*
    TODO: Proper cleanup. Maybe with a separate thread?
     */
    private static final Map<WeakReference<Object>, Set<javafx.beans.Observable>> BINDINGS = new ConcurrentHashMap<>();

    static {
        ThreadHelper.createPlatformThread("referenceGC", true, () -> {
                    while (true) {
                        for (ReferenceEntry reference : REFERENCES) {
                            if (reference.canGc()) {
                                /*
                                TODO: Figure out why some bindings are garbage collected, even if they shouldn't
                                 */
                                // REFERENCES.remove(reference);
                            }
                        }
                        ThreadHelper.sleep(1000);
                    }
                })
                .start();
    }

    public static <T, V> void bindExclusive(
            Property<V> selected, Map<V, ? extends Property<T>> map, Property<T> toBind) {
        selected.addListener((c, o, n) -> {
            toBind.unbind();
            toBind.bind(map.get(n));
        });

        toBind.bind(map.get(selected.getValue()));
    }

    public static void linkPersistently(Object source, Object target) {
        REFERENCES.add(new ReferenceEntry(new WeakReference<>(source), target));
    }

    public static <T extends Binding<?>> T persist(T binding) {
        var dependencies = new HashSet<javafx.beans.Observable>();
        while (dependencies.addAll(binding.getDependencies().stream()
                .map(o -> (javafx.beans.Observable) o)
                .toList())) {}
        dependencies.add(binding);
        BINDINGS.put(new WeakReference<>(binding), dependencies);
        return binding;
    }

    public static <T extends ListBinding<?>> T persist(T binding) {
        var dependencies = new HashSet<javafx.beans.Observable>();
        while (dependencies.addAll(binding.getDependencies().stream()
                .map(o -> (javafx.beans.Observable) o)
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

    public static <T, U> ObservableValue<U> map(
            ObservableValue<T> observableValue, Function<? super T, ? extends U> mapper) {
        return persist(Bindings.createObjectBinding(
                () -> {
                    return mapper.apply(observableValue.getValue());
                },
                observableValue));
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
        linkPersistently(observableValue, prop);
        return prop;
    }

    public static <T, U> ObservableValue<Boolean> anyMatch(List<? extends ObservableValue<Boolean>> l) {
        return BindingsHelper.persist(Bindings.createBooleanBinding(
                () -> {
                    return l.stream().anyMatch(booleanObservableValue -> booleanObservableValue.getValue());
                },
                l.toArray(ObservableValue[]::new)));
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

    public static <T, V> ObservableList<T> mappedContentBinding(ObservableList<V> l2, Function<V, T> map) {
        ObservableList<T> l1 = FXCollections.observableList(new ArrayList<>());
        Runnable runnable = () -> {
            setContent(l1, l2.stream().map(map).toList());
        };
        runnable.run();
        l2.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
        linkPersistently(l2, l1);
        return l1;
    }

    public static <T, V> ObservableList<T> cachedMappedContentBinding(ObservableList<V> l2, Function<V, T> map) {
        var cache = new HashMap<V, T>();

        ObservableList<T> l1 = FXCollections.observableList(new ArrayList<>());
        Runnable runnable = () -> {
            cache.keySet().removeIf(t -> !l2.contains(t));
            setContent(
                    l1,
                    l2.stream()
                            .map(v -> {
                                if (!cache.containsKey(v)) {
                                    cache.put(v, map.apply(v));
                                }

                                return cache.get(v);
                            })
                            .toList());
        };
        runnable.run();
        l2.addListener((ListChangeListener<? super V>) c -> {
            runnable.run();
        });
        linkPersistently(l2, l1);
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
        linkPersistently(all, l1);
        linkPersistently(shown, l1);
        return l1;
    }

    public static <T, U> ObservableValue<U> mappedBinding(
            ObservableValue<T> observableValue, Function<? super T, ? extends ObservableValue<? extends U>> mapper) {
        var binding = (Binding<U>) observableValue.flatMap(mapper);
        return persist(binding);
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

    //    public static <T,U> ObservableValue<U> mappedBinding(ObservableValue<T> observableValue, Function<? super T, ?
    // extends ObservableValue<? extends U>> mapper) {
    //        var v = new SimpleObjectProperty<U>();
    //        SimpleChangeListener.apply(observableValue, val -> {
    //            v.unbind();
    //            v.bind(mapper.apply(val));
    //        });
    //        return v;
    //    }

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
        linkPersistently(l2, l1);
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
        linkPersistently(l2, l1);
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

    @Value
    private static class ReferenceEntry {

        WeakReference<?> source;
        Object target;

        public boolean canGc() {
            return source.get() == null;
        }
    }
}
