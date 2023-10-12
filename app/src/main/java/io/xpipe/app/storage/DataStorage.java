package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.DataStoreId;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.FailableRunnable;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public abstract class DataStorage {

    public static final UUID ALL_CONNECTIONS_CATEGORY_UUID = UUID.fromString("bfb0b51a-e7a3-4ce4-8878-8d4cb5828d6c");
    public static final UUID ALL_SCRIPTS_CATEGORY_UUID = UUID.fromString("19024cf9-d192-41a9-88a6-a22694cf716a");
    public static final UUID PREDEFINED_SCRIPTS_CATEGORY_UUID = UUID.fromString("5faf1d71-0efc-4293-8b70-299406396973");
    public static final UUID CUSTOM_SCRIPTS_CATEGORY_UUID = UUID.fromString("d3496db5-b709-41f9-abc0-ee0a660fbab9");
    public static final UUID DEFAULT_CATEGORY_UUID = UUID.fromString("97458c07-75c0-4f9d-a06e-92d8cdf67c40");
    public static final UUID LOCAL_ID = UUID.fromString("f0ec68aa-63f5-405c-b178-9a4454556d6b");

    private static final String PERSIST_PROP = "io.xpipe.storage.persist";
    private static final String IMMUTABLE_PROP = "io.xpipe.storage.immutable";

    private static DataStorage INSTANCE;
    protected final Path dir;
    protected final List<DataStoreCategory> storeCategories;
    protected final List<DataStoreEntry> storeEntries;

    @Getter
    @Setter
    protected DataStoreCategory selectedCategory;

    @Getter
    private final List<StorageListener> listeners = new CopyOnWriteArrayList<>();

    public DataStorage() {
        this.dir = AppPrefs.get().storageDirectory().getValue();
        this.storeEntries = new CopyOnWriteArrayList<>();
        this.storeCategories = new CopyOnWriteArrayList<>();
    }

    protected void refreshValidities(boolean makeValid) {
        var changed = new AtomicBoolean(false);
        do {
            changed.set(false);
            storeEntries.forEach(dataStoreEntry -> {
                if (makeValid ? dataStoreEntry.tryMakeValid() : dataStoreEntry.tryMakeInvalid()) {
                    changed.set(true);
                }
            });
        } while (changed.get());
    }

    public DataStoreCategory getDefaultCategory() {
        return getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllCategory() {
        return getStoreCategoryIfPresent(ALL_CONNECTIONS_CATEGORY_UUID).orElseThrow();
    }

    private static boolean shouldPersist() {
        if (System.getProperty(PERSIST_PROP) != null) {
            return Boolean.parseBoolean(System.getProperty(PERSIST_PROP));
        }

        return true;
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = shouldPersist() ? new StandardStorage() : new ImpersistentStorage();
        INSTANCE.load();

        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.storageInit();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
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
        if (!(e.getStore() instanceof FixedHierarchyStore)) {
            return false;
        }

        e.setInRefresh(true);
        List<? extends DataStoreEntryRef<? extends FixedChildStore>> newChildren;
        try {
            newChildren = ((FixedHierarchyStore) (e.getStore())).listChildren(e);
            e.setInRefresh(false);
        } catch (Exception ex) {
            e.setInRefresh(false);
            ErrorEvent.fromThrowable(ex).handle();
            return false;
        }

        var oldChildren = getStoreEntries().stream().filter(other -> e.equals(other.getProvider().getLogicalParent(other))).toList();
        var toRemove = oldChildren.stream()
                .filter(entry -> newChildren.stream()
                        .noneMatch(
                                nc -> nc.getStore().getFixedId() == ((FixedChildStore) entry.getStore()).getFixedId()))
                .toList();
        var toAdd = newChildren.stream()
                .filter(entry -> oldChildren.stream()
                        .noneMatch(oc -> ((FixedChildStore) oc.getStore()).getFixedId()
                                == entry.getStore().getFixedId()))
                .toList();
        var toUpdate = oldChildren.stream()
                .map(entry -> {
                    var found = newChildren.stream()
                            .filter(nc -> nc.getStore().getFixedId() == ((FixedChildStore) entry.getStore()).getFixedId())
                            .findFirst()
                            .orElse(null);
                    return new Pair<>(entry, found);
                })
                .filter(en -> en.getValue() != null)
                .toList();

        if (newChildren.size() > 0) {
            e.setExpanded(true);
        }

        deleteWithChildren(toRemove.toArray(DataStoreEntry[]::new));
        addStoreEntriesIfNotPresent(toAdd.stream()
                .map(DataStoreEntryRef::get)
                .toArray(DataStoreEntry[]::new));
        toUpdate.forEach(pair -> {
            propagateUpdate(
                    () -> {
                        pair.getKey().setStoreInternal(pair.getValue().getStore(), false);
                    },
                    pair.getKey());
        });
            saveAsync();
        return !newChildren.isEmpty();
    }

    public void deleteWithChildren(DataStoreEntry... entries) {
        var toDelete = Arrays.stream(entries)
                .flatMap(entry -> {
                    // Reverse to delete deepest children first
                    var ordered = getStoreChildren(entry, true);
                    Collections.reverse(ordered);
                    ordered.add(entry);
                    return ordered.stream();
                })
                .toList();

        toDelete.forEach(entry -> entry.finalizeEntry());
        this.storeEntries.removeAll(toDelete);
        this.listeners.forEach(l -> l.onStoreRemove(toDelete.toArray(DataStoreEntry[]::new)));
        refreshValidities(false);
        saveAsync();
    }

    public void deleteChildren(DataStoreEntry e, boolean deep) {
        // Reverse to delete deepest children first
        var ordered = getStoreChildren(e, deep);
        Collections.reverse(ordered);

        ordered.forEach(entry -> entry.finalizeEntry());
        this.storeEntries.removeAll(ordered);
        this.listeners.forEach(l -> l.onStoreRemove(ordered.toArray(DataStoreEntry[]::new)));
        refreshValidities(false);
        saveAsync();
    }

    public boolean isRootEntry(DataStoreEntry entry) {
        var noParent = DataStorage.get()
                .getDisplayParent(entry)
                .isEmpty();
        var diffParentCategory = DataStorage.get()
                .getDisplayParent(entry)
                .map(p -> !p.getCategoryUuid().equals(entry.getCategoryUuid()))
                .orElse(false);
        return noParent || diffParentCategory;
    }

    public DataStoreEntry getRootForEntry(DataStoreEntry entry) {
        if (entry == null) {
            return null;
        }

        if (isRootEntry(entry)) {
            return entry;
        }

        var current = entry;
        Optional<DataStoreEntry> parent;
        while ((parent = getDisplayParent(current)).isPresent()) {
            current = parent.get();
            if (isRootEntry(current)) {
                break;
            }
        }

        return current;
    }

    public Optional<DataStoreEntry> getDisplayParent(DataStoreEntry entry) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return Optional.empty();
        }

        try {
            var provider = entry.getProvider();
            return Optional.ofNullable(provider.getDisplayParent(entry)).filter(dataStoreEntry -> storeEntries.contains(dataStoreEntry));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public List<DataStoreEntry> getStoreChildren(DataStoreEntry entry, boolean deep) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return List.of();
        }

        var entries = getStoreEntries();
        if (!entries.contains(entry)) {
            return List.of();
        }

        var children = new ArrayList<>(entries.stream()
                .filter(other -> {
                    if (other.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                        return false;
                    }

                    var parent = getDisplayParent(other);
                    return parent.isPresent()
                            && parent.get().equals(entry);
                })
                .toList());

        if (deep) {
            for (DataStoreEntry dataStoreEntry : new ArrayList<>(children)) {
                children.addAll(getStoreChildren(dataStoreEntry, true));
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

    protected Path getCategoriesDir() {
        return dir.resolve("categories");
    }

    public List<DataStore> getUsableStores() {
        return new ArrayList<>(getStoreEntries().stream()
                .filter(entry -> entry.getValidity().isUsable())
                .map(DataStoreEntry::getStore)
                .toList());
    }

    public DataStoreEntry getStoreEntry(@NonNull String name, boolean acceptDisabled) {
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
        names.add(entry.getName().replaceAll(":", "_"));

        DataStoreEntry current = entry;
        while ((current = getDisplayParent(current).orElse(null)) != null) {
            if (new LocalStore().equals(current.getStore())) {
                break;
            }

            names.add(0, current.getName().replaceAll(":", "_"));
        }

        return DataStoreId.create(names.toArray(String[]::new));
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStoreId id) {
        var current = getStoreEntryIfPresent(id.getNames().get(0));
        if (current.isPresent()) {
            for (int i = 1; i < id.getNames().size(); i++) {
                var children = getStoreChildren(current.get(), false);
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

    public DataStoreEntry getStoreEntry(@NonNull DataStore store) {
        return getStoreEntryIfPresent(store)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        return storeEntries.stream()
                .filter(n -> n.getStore() == store || (n.getStore() != null
                            && Objects.equals(store.getClass(), n.getStore().getClass())
                            && store.equals(n.getStore())))
                .findFirst();
    }

    public abstract boolean supportsSharing();

    public DataStoreCategory getRootCategory(DataStoreCategory category) {
        DataStoreCategory last = category;
        DataStoreCategory p = category;
        while ((p = DataStorage.get()
                .getStoreCategoryIfPresent(p.getParentCategory())
                .orElse(null))
                != null) {
            last = p;
        }
        return last;
    }

    public Optional<DataStoreCategory> getStoreCategoryIfPresent(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }

        return storeCategories.stream()
                .filter(n -> {
                    return Objects.equals(n.getUuid(), uuid);
                })
                .findFirst();
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull String name) {
        return storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public void updateEntry(DataStoreEntry entry, DataStoreEntry newEntry) {
        var oldParent = DataStorage.get().getDisplayParent(entry);
        var newParent = DataStorage.get().getDisplayParent(newEntry);
        var diffParent = Objects.equals(oldParent, newParent);

        propagateUpdate(
                () -> {
                    newEntry.finalizeEntry();

                    var children = getStoreChildren(entry, true);
                    if (!diffParent) {
                        var toRemove = Stream.concat(Stream.of(entry), children.stream())
                                .toArray(DataStoreEntry[]::new);
                        listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));
                    }

                    entry.applyChanges(newEntry);
                    entry.initializeEntry();

                    if (!diffParent) {
                        var toAdd = Stream.concat(Stream.of(entry), children.stream())
                                .toArray(DataStoreEntry[]::new);
                        listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
                        refreshValidities(true);
                    }
                },
                entry);
    }

    public void updateCategory(DataStoreEntry entry, DataStoreCategory newCategory) {
        propagateUpdate(
                () -> {
                    var children = getStoreChildren(entry, true);
                    var toRemove =
                            Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
                    listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));

                    entry.setCategoryUuid(newCategory.getUuid());
                    children.forEach(child -> child.setCategoryUuid(newCategory.getUuid()));

                    var toAdd =
                            Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
                    listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
                },
                entry);
    }

    <T extends Throwable> void propagateUpdate(FailableRunnable<T> runnable, DataStoreEntry origin) throws T {
        var children = getStoreChildren(origin, true);
        runnable.run();
        children.forEach(entry -> {
            entry.refresh();
        });
    }

    public DataStoreCategory addStoreCategoryIfNotPresent(@NonNull DataStoreCategory cat) {
        if (storeCategories.contains(cat)) {
            return cat;
        }

        var byId = getStoreCategoryIfPresent(cat.getUuid()).orElse(null);
        if (byId != null) {
            return byId;
        }

        addStoreCategory(cat);
        return cat;
    }

    public void addStoreCategory(@NonNull DataStoreCategory cat) {
        cat.setDirectory(getCategoriesDir().resolve(cat.getUuid().toString()));
        this.storeCategories.add(cat);
        saveAsync();

        this.listeners.forEach(l -> l.onCategoryAdd(cat));
    }

    public DataStoreEntry addStoreEntryIfNotPresent(@NonNull DataStoreEntry e) {
        if (storeEntries.contains(e)) {
            return e;
        }

        var byId = getStoreEntryIfPresent(e.getUuid()).orElse(null);
        if (byId != null) {
            return byId;
        }

        if (e.getValidity().isUsable()) {
            var displayParent = e.getProvider().getDisplayParent(e);
            if (displayParent != null) {
                displayParent.setExpanded(true);
                addStoreEntryIfNotPresent(displayParent);
            }
        }

        e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
        this.storeEntries.add(e);
        saveAsync();

        this.listeners.forEach(l -> l.onStoreAdd(e));
        e.initializeEntry();
        refreshValidities(true);
        return e;
    }

    public DataStoreEntry getOrCreateNewEntry(String name, DataStore store) {
        var found = getStoreEntryIfPresent(store);
        if (found.isPresent()) {
            return found.get();
        }

        return DataStoreEntry.createNew(UUID.randomUUID(), selectedCategory.getUuid(), name, store);
    }

    public void addStoreEntriesIfNotPresent(@NonNull DataStoreEntry... es) {
        for (DataStoreEntry e : es) {
            if (storeEntries.contains(e) || getStoreEntryIfPresent(e.getStore()).isPresent()) {
                return;
            }

            var displayParent = e.getProvider().getDisplayParent(e);
            if (displayParent != null) {
                addStoreEntryIfNotPresent(displayParent);
            }

            e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
            this.storeEntries.add(e);
        }
        this.listeners.forEach(l -> l.onStoreAdd(es));
        for (DataStoreEntry e : es) {
            e.initializeEntry();
        }
        refreshValidities(true);
        saveAsync();
    }

    public DataStoreEntry addStoreIfNotPresent(@NonNull String name, DataStore store) {
        var f = getStoreEntryIfPresent(store);
        if (f.isPresent()) {
            return f.get();
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), selectedCategory.getUuid(), name, store);
        addStoreEntryIfNotPresent(e);
        return e;
    }

    public Optional<String> getStoreDisplayName(DataStore store) {
        if (store == null) {
            return Optional.empty();
        }

        return getStoreEntryIfPresent(store).map(dataStoreEntry -> dataStoreEntry.getName());
    }

    public String getStoreDisplayName(DataStoreEntry store) {
        if (store == null) {
            return "?";
        }

        return store.getProvider().browserDisplayName(store.getStore());
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        propagateUpdate(
                () -> {
                    store.finalizeEntry();
                    this.storeEntries.remove(store);
                },
                store);
        this.listeners.forEach(l -> l.onStoreRemove(store));
        refreshValidities(false);
        saveAsync();
    }

    public void deleteStoreCategory(@NonNull DataStoreCategory cat) {
        if (cat.getUuid().equals(DEFAULT_CATEGORY_UUID) || cat.getUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID)) {
            return;
        }

        storeEntries.forEach(entry -> {
            if (entry.getCategoryUuid().equals(cat.getUuid())) {
                entry.setCategoryUuid(DEFAULT_CATEGORY_UUID);
            }
        });

        storeCategories.remove(cat);
        saveAsync();
        this.listeners.forEach(l -> l.onCategoryRemove(cat));
    }

    public void addListener(StorageListener l) {
        this.listeners.add(l);
    }

    public abstract void load();

    public void saveAsync() {
        // TODO: Don't make this a daemon thread to guarantee proper saving
        ThreadHelper.unstarted(this::save).start();
    }

    public abstract void save();

    public Optional<DataStoreEntry> getStoreEntryIfPresent(UUID id) {
        return storeEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public DataStoreEntry getStoreEntry(UUID id) {
        return getStoreEntryIfPresent(id).orElseThrow();
    }

    public DataStoreEntry local() {
        return getStoreEntryIfPresent(LOCAL_ID).orElse(null);
    }

    public List<DataStoreEntry> getStoreEntries() {
        return new ArrayList<>(storeEntries);
    }

    public List<DataStoreCategory> getStoreCategories() {
        return new ArrayList<>(storeCategories);
    }
}
