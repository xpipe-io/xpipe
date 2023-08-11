package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.source.DataStoreId;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.FixedHierarchyStore;
import io.xpipe.core.util.FailableRunnable;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public abstract class DataStorage {

    private static final String PERSIST_PROP = "io.xpipe.storage.persist";
    private static final String IMMUTABLE_PROP = "io.xpipe.storage.immutable";

    private static DataStorage INSTANCE;
    protected final Path dir;
    protected final List<DataStoreEntry> storeEntries;

    @Getter
    private final List<StorageListener> listeners = new ArrayList<>();

    public DataStorage() {
        this.dir = AppPrefs.get().storageDirectory().getValue();
        this.storeEntries = new ArrayList<>();
    }

    private static boolean shouldPersist() {
        if (System.getProperty(PERSIST_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(PERSIST_PROP));
        }

        return true;
    }

    public static void init() {
        INSTANCE = shouldPersist() ? new StandardStorage() : new ImpersistentStorage();
        INSTANCE.load();

        INSTANCE.storeEntries.forEach(entry -> entry.simpleRefresh());

        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.storageInit();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });

        INSTANCE.save();
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.save();
        INSTANCE = null;
    }

    public static DataStorage get() {
        return INSTANCE;
    }

    public boolean refreshChildren(DataStoreEntry e) {
        return refreshChildren(e, null);
    }

    public synchronized boolean refreshChildren(DataStoreEntry e, DataStore newValue) {
        if (!(e.getStore() instanceof FixedHierarchyStore)) {
            return false;
        }

        var oldChildren = getStoreChildren(e, false, false);
        Map<String, FixedChildStore> newChildren;
        try {
            newChildren = ((FixedHierarchyStore) (newValue != null ? newValue : e.getStore())).listChildren();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return false;
        }

        var toRemove = oldChildren.stream()
                .filter(entry -> newChildren.entrySet().stream()
                        .noneMatch(
                                nc -> nc.getValue().getFixedId() == ((FixedChildStore) entry.getStore()).getFixedId()))
                .toList();
        var toAdd = newChildren.entrySet().stream()
                .filter(entry -> oldChildren.stream()
                        .noneMatch(oc -> ((FixedChildStore) oc.getStore()).getFixedId()
                                == entry.getValue().getFixedId()))
                .toList();
        var toUpdate = oldChildren.stream()
                .map(entry -> {
                    FixedChildStore found = newChildren.values().stream()
                            .filter(nc -> nc.getFixedId() == ((FixedChildStore) entry.getStore()).getFixedId())
                            .findFirst()
                            .orElse(null);
                    return new Pair<>(entry, found);
                })
                .filter(en -> en.getValue() != null)
                .toList();

        if (newValue != null) {
            e.setStoreInternal(newValue, false);
        }

        deleteWithChildren(toRemove.toArray(DataStoreEntry[]::new));
        addStoreEntries(toAdd.stream()
                .map(stringDataStoreEntry -> DataStoreEntry.createNew(
                        UUID.randomUUID(), stringDataStoreEntry.getKey(), stringDataStoreEntry.getValue()))
                .toArray(DataStoreEntry[]::new));
        toUpdate.forEach(entry -> {
            propagateUpdate(
                    () -> {
                        entry.getKey().setStoreInternal(entry.getValue(), false);
                    },
                    entry.getKey());
        });
        save();
        return !newChildren.isEmpty();
    }

    public synchronized void deleteWithChildren(DataStoreEntry... entries) {
        var toDelete = Arrays.stream(entries)
                .flatMap(entry -> {
                    // Reverse to delete deepest children first
                    var ordered = getStoreChildren(entry, false, true);
                    Collections.reverse(ordered);
                    return ordered.stream();
                })
                .toList();

        synchronized (this) {
            toDelete.forEach(entry -> entry.finalizeEntry());
            this.storeEntries.removeAll(toDelete);
            this.listeners.forEach(l -> l.onStoreRemove(toDelete.toArray(DataStoreEntry[]::new)));
        }
        save();
    }

    public synchronized void deleteChildren(DataStoreEntry e, boolean deep) {
        // Reverse to delete deepest children first
        var ordered = getStoreChildren(e, false, deep);
        Collections.reverse(ordered);

        synchronized (this) {
            ordered.forEach(entry -> entry.finalizeEntry());
            this.storeEntries.removeAll(ordered);
            this.listeners.forEach(l -> l.onStoreRemove(ordered.toArray(DataStoreEntry[]::new)));
        }
        save();
    }

    public synchronized Optional<DataStoreEntry> getParent(DataStoreEntry entry, boolean display) {
        if (entry.getState() == DataStoreEntry.State.LOAD_FAILED) {
            return Optional.empty();
        }

        try {
            var provider = entry.getProvider();
            var parent =
                    display ? provider.getDisplayParent(entry.getStore()) : provider.getLogicalParent(entry.getStore());
            return parent != null ? getStoreEntryIfPresent(parent) : Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public synchronized List<DataStoreEntry> getStoreChildren(DataStoreEntry entry, boolean display, boolean deep) {
        if (entry.getState() == DataStoreEntry.State.LOAD_FAILED) {
            return List.of();
        }

        var children = new ArrayList<>(getStoreEntries().stream()
                .filter(other -> {
                    if (other.getState() == DataStoreEntry.State.LOAD_FAILED) {
                        return false;
                    }

                    var parent = getParent(other, display);
                    return parent.isPresent()
                            && entry.getStore()
                                    .getClass()
                                    .equals(parent.get().getStore().getClass())
                            && entry.getStore().equals(parent.get().getStore());
                })
                .toList());

        if (deep) {
            for (DataStoreEntry dataStoreEntry : new ArrayList<>(children)) {
                children.addAll(getStoreChildren(dataStoreEntry, display, true));
            }
        }

        return children;
    }

    public abstract Path getInternalStreamPath(@NonNull UUID uuid);

    private void checkImmutable() {
        if (System.getProperty(IMMUTABLE_PROP) != null) {
            if (Boolean.parseBoolean(System.getProperty(IMMUTABLE_PROP))) {
                throw new IllegalStateException("Storage is immutable");
            }
        }
    }

    protected Path getStoresDir() {
        return dir.resolve("stores");
    }

    protected Path getStreamsDir() {
        return dir.resolve("streams");
    }

    public synchronized List<DataStore> getUsableStores() {
        return new ArrayList<>(getStoreEntries().stream()
                .filter(entry -> entry.getState().isUsable())
                .map(DataStoreEntry::getStore)
                .toList());
    }

    public synchronized void renameStoreEntry(DataStoreEntry entry, String name) {
        if (getStoreEntryIfPresent(name).isPresent()) {
            throw new IllegalArgumentException("Store with name " + name + " already exists");
        }

        entry.setName(name);
    }

    public synchronized DataStoreEntry getStoreEntry(@NonNull String name, boolean acceptDisabled) {
        var entry = storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store with name " + name + " not found"));
        if (!acceptDisabled && entry.isDisabled()) {
            throw new IllegalArgumentException("Store with name " + name + " is disabled");
        }
        return entry;
    }

    public DataStoreId getId(DataStoreEntry entry) {
        var names = new ArrayList<String>();
        names.add(entry.getName());

        DataStoreEntry current = entry;
        while ((current = getParent(current, false).orElse(null)) != null) {
            if (new LocalStore().equals(current.getStore())) {
                break;
            }

            names.add(0, current.getName());
        }

        return DataStoreId.create(names.toArray(String[]::new));
    }

    public synchronized DataStoreEntry getStoreEntry(@NonNull DataStore store) {
        return storeEntries.stream()
                .filter(n -> n.getStore() != null
                        && Objects.equals(store.getClass(), n.getStore().getClass())
                        && store.equals(n.getStore()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStoreId id) {
        var current = getStoreEntryIfPresent(id.getNames().get(0));
        if (current.isPresent()) {
            for (int i = 1; i < id.getNames().size(); i++) {
                var children = getStoreChildren(current.get(), false, false);
                int finalI = i;
                current = children.stream()
                        .filter(dataStoreEntry -> dataStoreEntry
                                .getName()
                                .equalsIgnoreCase(id.getNames().get(finalI)))
                        .findFirst();
                if (current.isEmpty()) {
                    break;
                }
            }

            if (current.isPresent()) {
                return current;
            }
        }
        return Optional.empty();
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        return storeEntries.stream()
                .filter(n -> {
                    return n.getStore() != null
                            && store.getClass().equals(n.getStore().getClass())
                            && store.equals(n.getStore());
                })
                .findFirst();
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull String name) {
        return storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public boolean setAndRefresh(DataStoreEntry entry, DataStore s) {
        var old = entry.getStore();
        deleteChildren(entry, true);
        try {
            entry.setStoreInternal(s, false);
            entry.refresh(true);
            return DataStorage.get().refreshChildren(entry, s);
        } catch (Exception e) {
            entry.setStoreInternal(old, false);
            entry.simpleRefresh();
            return false;
        }
    }

    public void updateEntry(DataStoreEntry entry, DataStoreEntry newEntry) {
        var oldParent = DataStorage.get().getParent(entry, false);
        var newParent = DataStorage.get().getParent(newEntry, false);

        propagateUpdate(
                () -> {
                    newEntry.finalizeEntry();

                    var children = getStoreChildren(entry, false, true);
                    if (!Objects.equals(oldParent, newParent)) {
                        var toRemove = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
                        listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));
                    }

                    entry.applyChanges(newEntry);
                    entry.initializeEntry();

                    if (!Objects.equals(oldParent, newParent)) {
                        var toAdd = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
                        listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
                    }
                },
                entry);
    }

    public void refreshAsync(DataStoreEntry element, boolean deep) {
        ThreadHelper.runAsync(() -> {
            try {
                propagateUpdate(() -> element.refresh(deep), element);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).reportable(false).handle();
            }
            save();
        });
    }

    <T extends Throwable> void propagateUpdate(FailableRunnable<T> runnable, DataStoreEntry origin) throws T {
        var children = getStoreChildren(origin, false, true);
        runnable.run();
        children.forEach(entry -> {
            entry.simpleRefresh();
        });
    }

    public void addStoreEntry(@NonNull DataStoreEntry e) {
        e.getProvider().preAdd(e.getStore());
        synchronized (this) {
            e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
            this.storeEntries.add(e);
        }
        save();

        this.listeners.forEach(l -> l.onStoreAdd(e));
        e.initializeEntry();
    }

    public void addStoreEntries(@NonNull DataStoreEntry... es) {
        synchronized (this) {
            for (DataStoreEntry e : es) {
                e.getProvider().preAdd(e.getStore());
                e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
                this.storeEntries.add(e);
            }
            this.listeners.forEach(l -> l.onStoreAdd(es));
            for (DataStoreEntry e : es) {
                e.initializeEntry();
            }
        }
        save();
    }

    public DataStoreEntry addStoreEntryIfNotPresent(@NonNull String name, DataStore store) {
        var found = getStoreEntryIfPresent(store);
        if (found.isPresent()) {
            return found.get();
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), name, store);
        addStoreEntry(e);
        return e;
    }

    public DataStoreEntry addStoreEntry(@NonNull String name, DataStore store) {
        var e = DataStoreEntry.createNew(UUID.randomUUID(), name, store);
        addStoreEntry(e);
        return e;
    }

    public Optional<String> getStoreDisplayName(DataStore store) {
        if (store == null) {
            return Optional.empty();
        }

        return DataStorage.get().getStoreEntries().stream()
                .filter(entry -> !entry.isDisabled() && entry.getStore().equals(store))
                .findFirst()
                .map(entry -> entry.getName());
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        propagateUpdate(
                () -> {
                    store.finalizeEntry();
                    synchronized (this) {
                        this.storeEntries.remove(store);
                    }
                },
                store);
        save();
        this.listeners.forEach(l -> l.onStoreRemove(store));
    }

    public synchronized void addListener(StorageListener l) {
        this.listeners.add(l);
    }

    public abstract void load();

    public abstract void save();

    public synchronized Optional<DataStoreEntry> getStoreEntry(UUID id) {
        return storeEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public synchronized List<DataStoreEntry> getStoreEntries() {
        return new ArrayList<>(storeEntries);
    }
}
