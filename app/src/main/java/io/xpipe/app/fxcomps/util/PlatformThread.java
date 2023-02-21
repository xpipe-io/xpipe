package io.xpipe.app.fxcomps.util;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unchecked")
public class PlatformThread {

    public static Observable sync(Observable o) {
        return new Observable() {

            private final Map<InvalidationListener, InvalidationListener> invListenerMap = new ConcurrentHashMap<>();

            @Override
            public void addListener(InvalidationListener listener) {
                InvalidationListener l = o -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.invalidated(o));
                };

                invListenerMap.put(listener, l);
                o.addListener(l);
            }

            @Override
            public void removeListener(InvalidationListener listener) {
                o.removeListener(invListenerMap.getOrDefault(listener, listener));
            }
        };
    }

    public static <T> ObservableValue<T> sync(ObservableValue<T> ov) {
        return new ObservableValue<>() {

            private final Map<ChangeListener<? super T>, ChangeListener<? super T>> changeListenerMap =
                    new ConcurrentHashMap<>();
            private final Map<InvalidationListener, InvalidationListener> invListenerMap = new ConcurrentHashMap<>();

            @Override
            public void addListener(ChangeListener<? super T> listener) {
                ChangeListener<? super T> l = (c, o, n) -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.changed(c, o, n));
                };

                changeListenerMap.put(listener, l);
                ov.addListener(l);
            }

            @Override
            public void removeListener(ChangeListener<? super T> listener) {
                ov.removeListener(changeListenerMap.getOrDefault(listener, listener));
            }

            @Override
            public T getValue() {
                return ov.getValue();
            }

            @Override
            public void addListener(InvalidationListener listener) {
                InvalidationListener l = o -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.invalidated(o));
                };

                invListenerMap.put(listener, l);
                ov.addListener(l);
            }

            @Override
            public void removeListener(InvalidationListener listener) {
                ov.removeListener(invListenerMap.getOrDefault(listener, listener));
            }
        };
    }

    public static <T> ObservableList<T> sync(ObservableList<T> ol) {
        return new ObservableList<>() {

            private final Map<ListChangeListener<? super T>, ListChangeListener<? super T>> listChangeListenerMap =
                    new ConcurrentHashMap<>();
            private final Map<InvalidationListener, InvalidationListener> invListenerMap = new ConcurrentHashMap<>();

            @Override
            public void addListener(ListChangeListener<? super T> listener) {
                ListChangeListener<? super T> l = (lc) -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.onChanged(lc));
                };

                listChangeListenerMap.put(listener, l);
                ol.addListener(l);
            }

            @Override
            public void removeListener(ListChangeListener<? super T> listener) {
                ol.removeListener(listChangeListenerMap.getOrDefault(listener, listener));
            }

            @Override
            public boolean addAll(T... elements) {
                return ol.addAll(elements);
            }

            @Override
            public boolean setAll(T... elements) {
                return ol.setAll(elements);
            }

            @Override
            public boolean setAll(Collection<? extends T> col) {
                return ol.setAll(col);
            }

            @Override
            public boolean removeAll(T... elements) {
                return ol.removeAll(elements);
            }

            @Override
            public boolean retainAll(T... elements) {
                return ol.retainAll(elements);
            }

            @Override
            public void remove(int from, int to) {
                ol.remove(from, to);
            }

            @Override
            public int size() {
                return ol.size();
            }

            @Override
            public boolean isEmpty() {
                return ol.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return ol.contains(o);
            }

            @Override
            public Iterator<T> iterator() {
                return ol.iterator();
            }

            @Override
            public Object[] toArray() {
                return ol.toArray();
            }

            @Override
            public <T1> T1[] toArray(T1[] a) {
                return ol.toArray(a);
            }

            @Override
            public boolean add(T t) {
                return ol.add(t);
            }

            @Override
            public boolean remove(Object o) {
                return ol.remove(o);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return ol.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends T> c) {
                return ol.addAll(c);
            }

            @Override
            public boolean addAll(int index, Collection<? extends T> c) {
                return ol.addAll(index, c);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return ol.removeAll(c);
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return ol.retainAll(c);
            }

            @Override
            public void clear() {
                ol.clear();
            }

            @Override
            public T get(int index) {
                return ol.get(index);
            }

            @Override
            public T set(int index, T element) {
                return ol.set(index, element);
            }

            @Override
            public void add(int index, T element) {
                ol.add(index, element);
            }

            @Override
            public T remove(int index) {
                return ol.remove(index);
            }

            @Override
            public int indexOf(Object o) {
                return ol.indexOf(o);
            }

            @Override
            public int lastIndexOf(Object o) {
                return ol.lastIndexOf(o);
            }

            @Override
            public ListIterator<T> listIterator() {
                return ol.listIterator();
            }

            @Override
            public ListIterator<T> listIterator(int index) {
                return ol.listIterator(index);
            }

            @Override
            public List<T> subList(int fromIndex, int toIndex) {
                return ol.subList(fromIndex, toIndex);
            }

            @Override
            public void addListener(InvalidationListener listener) {
                InvalidationListener l = o -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.invalidated(o));
                };

                invListenerMap.put(listener, l);
                ol.addListener(l);
            }

            @Override
            public void removeListener(InvalidationListener listener) {
                ol.removeListener(invListenerMap.getOrDefault(listener, listener));
            }
        };
    }

    public static void runLaterIfNeeded(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public static void runLaterIfNeededBlocking(Runnable r) {
        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                r.run();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            r.run();
        }
    }

    public static void alwaysRunLaterBlocking(Runnable r) {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            r.run();
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }
}
