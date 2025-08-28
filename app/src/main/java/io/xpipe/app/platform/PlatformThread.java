package io.xpipe.app.platform;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import lombok.NonNull;

import java.util.*;
import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unchecked")
public class PlatformThread {

    public static <T> ObservableValue<T> sync(ObservableValue<T> ov) {
        Objects.requireNonNull(ov);
        ObservableValue<T> obs = new ObservableValue<>() {

            private final Map<ChangeListener<? super T>, ChangeListener<? super T>> changeListenerMap = new HashMap<>();
            private final Map<InvalidationListener, InvalidationListener> invListenerMap = new HashMap<>();

            @Override
            public synchronized void addListener(ChangeListener<? super T> listener) {
                ChangeListener<? super T> l = (c, o, n) -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.changed(c, o, n));
                };

                changeListenerMap.put(listener, l);
                ov.addListener(l);
            }

            @Override
            public synchronized void removeListener(ChangeListener<? super T> listener) {
                var r = changeListenerMap.remove(listener);
                if (r != null) {
                    ov.removeListener(r);
                }
            }

            @Override
            public T getValue() {
                return ov.getValue();
            }

            @Override
            public synchronized void addListener(InvalidationListener listener) {
                InvalidationListener l = o -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.invalidated(o));
                };

                invListenerMap.put(listener, l);
                ov.addListener(l);
            }

            @Override
            public synchronized void removeListener(InvalidationListener listener) {
                var r = invListenerMap.remove(listener);
                if (r != null) {
                    ov.removeListener(r);
                }
            }
        };
        return obs;
    }

    public static <T> ObservableList<T> sync(ObservableList<T> ol) {
        Objects.requireNonNull(ol);
        ObservableList<T> obs = new ObservableList<>() {

            private final Map<ListChangeListener<? super T>, ListChangeListener<? super T>> listChangeListenerMap =
                    new HashMap<>();
            private final Map<InvalidationListener, InvalidationListener> invListenerMap = new HashMap<>();

            @Override
            public synchronized void addListener(ListChangeListener<? super T> listener) {
                ListChangeListener<? super T> l = (lc) -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.onChanged(lc));
                };

                listChangeListenerMap.put(listener, l);
                ol.addListener(l);
            }

            @Override
            public synchronized void removeListener(ListChangeListener<? super T> listener) {
                var r = listChangeListenerMap.remove(listener);
                if (r != null) {
                    ol.removeListener(r);
                }
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
            public @NonNull Iterator<T> iterator() {
                return ol.iterator();
            }

            @Override
            public Object @NonNull [] toArray() {
                return ol.toArray();
            }

            @Override
            public <T1> T1 @NonNull [] toArray(T1 @NonNull [] a) {
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
            public boolean containsAll(@NonNull Collection<?> c) {
                return ol.containsAll(c);
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends T> c) {
                return ol.addAll(c);
            }

            @Override
            public boolean addAll(int index, @NonNull Collection<? extends T> c) {
                return ol.addAll(index, c);
            }

            @Override
            public boolean removeAll(@NonNull Collection<?> c) {
                return ol.removeAll(c);
            }

            @Override
            public boolean retainAll(@NonNull Collection<?> c) {
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
            public @NonNull ListIterator<T> listIterator() {
                return ol.listIterator();
            }

            @Override
            public @NonNull ListIterator<T> listIterator(int index) {
                return ol.listIterator(index);
            }

            @Override
            public @NonNull List<T> subList(int fromIndex, int toIndex) {
                return ol.subList(fromIndex, toIndex);
            }

            @Override
            public synchronized void addListener(InvalidationListener listener) {
                InvalidationListener l = o -> {
                    PlatformThread.runLaterIfNeeded(() -> listener.invalidated(o));
                };

                invListenerMap.put(listener, l);
                ol.addListener(l);
            }

            @Override
            public synchronized void removeListener(InvalidationListener listener) {
                var r = invListenerMap.remove(listener);
                if (r != null) {
                    ol.removeListener(r);
                }
            }
        };
        return obs;
    }

    private static boolean canRunPlatform() {
        if (PlatformState.getCurrent() != PlatformState.RUNNING) {
            return false;
        }

        if (OperationMode.isInShutdown()) {
            return false;
        }

        return true;
    }

    public static void enterNestedEventLoop(Object key) {
        if (!Platform.canStartNestedEventLoop()) {
            return;
        }

        try {
            Platform.enterNestedEventLoop(key);
        } catch (IllegalStateException ex) {
            // We might be in an animation or layout call
            ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
        }
    }

    public static void exitNestedEventLoop(Object key) {
        try {
            Platform.exitNestedEventLoop(key, null);
        } catch (IllegalArgumentException ex) {
            // The event loop might have died somehow
            // Or we passed an invalid key
            ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
        }
    }

    public static void runNestedLoopIteration() {
        if (!Platform.canStartNestedEventLoop()) {
            return;
        }

        var key = new Object();
        Platform.runLater(() -> {
            exitNestedEventLoop(key);
        });
        enterNestedEventLoop(key);
    }

    public static void runLaterIfNeeded(Runnable r) {
        if (!canRunPlatform()) {
            return;
        }

        Runnable catcher = () -> {
            try {
                r.run();
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).handle();
            }
        };

        if (Platform.isFxApplicationThread()) {
            catcher.run();
        } else {
            Platform.runLater(catcher);
        }
    }

    public static void runLaterIfNeededBlocking(Runnable r) {
        if (!canRunPlatform()) {
            return;
        }

        Runnable catcher = () -> {
            try {
                r.run();
            } catch (Throwable t) {
                ErrorEventFactory.fromThrowable(t).handle();
            }
        };

        if (!Platform.isFxApplicationThread()) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                catcher.run();
                latch.countDown();
            });
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }
        } else {
            catcher.run();
        }
    }
}
