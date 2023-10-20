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
import io.xpipe.core.util.UuidHelper;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
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

    @Getter
    protected final List<DataStoreCategory> storeCategories;

    @Getter
    protected final Set<DataStoreEntry> storeEntries;

    @Getter
    @Setter
    protected DataStoreCategory selectedCategory;

    @Getter
    private final List<StorageListener> listeners = new CopyOnWriteArrayList<>();

    public DataStorage() {
        this.dir = AppPrefs.get().storageDirectory().getValue();
        this.storeEntries = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.storeCategories = new CopyOnWriteArrayList<>();
    }

    public DataStoreCategory getDefaultCategory() {
        return getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllConnectionsCategory() {
        return getStoreCategoryIfPresent(ALL_CONNECTIONS_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllScriptsCategory() {
        return getStoreCategoryIfPresent(ALL_SCRIPTS_CATEGORY_UUID).orElseThrow();
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

    protected Path getStoresDir() {
        return dir.resolve("stores");
    }

    protected Path getStreamsDir() {
        return dir.resolve("streams");
    }

    protected Path getCategoriesDir() {
        return dir.resolve("categories");
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

    public abstract boolean supportsSharing();

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

    public void updateEntry(DataStoreEntry entry, DataStoreEntry newEntry) {
        var oldParent = DataStorage.get().getDisplayParent(entry);
        var newParent = DataStorage.get().getDisplayParent(newEntry);
        var diffParent = Objects.equals(oldParent, newParent);

        newEntry.finalizeEntry();

        var children = getDeepStoreChildren(entry);
        if (!diffParent) {
            var toRemove = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
            listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));
        }

        entry.applyChanges(newEntry);
        entry.initializeEntry();

        if (!diffParent) {
            var toAdd = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
            listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
            refreshValidities(true);
        }

        saveAsync();
    }

    public void updateCategory(DataStoreEntry entry, DataStoreCategory newCategory) {
        var children = getDeepStoreChildren(entry);
        var toRemove = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
        listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));

        entry.setCategoryUuid(newCategory.getUuid());
        children.forEach(child -> child.setCategoryUuid(newCategory.getUuid()));

        var toAdd = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
        listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
        saveAsync();
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

        var oldChildren = getStoreEntries().stream()
                .filter(other -> e.equals(getDisplayParent(other).orElse(null)))
                .toList();
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
                            .filter(nc ->
                                    nc.getStore().getFixedId() == ((FixedChildStore) entry.getStore()).getFixedId())
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
        addStoreEntriesIfNotPresent(toAdd.stream().map(DataStoreEntryRef::get).toArray(DataStoreEntry[]::new));
        toUpdate.forEach(pair -> {
            pair.getKey().setStoreInternal(pair.getValue().getStore(), false);
        });
        e.setChildrenCache(newChildren.stream().map(DataStoreEntryRef::get).collect(Collectors.toSet()));
        saveAsync();
        return !newChildren.isEmpty();
    }

    public void deleteChildren(DataStoreEntry e) {
        var c = getDeepStoreChildren(e);
        c.forEach(entry -> entry.finalizeEntry());
        this.storeEntries.removeAll(c);
        this.listeners.forEach(l -> l.onStoreRemove(c.toArray(DataStoreEntry[]::new)));
        refreshValidities(false);
        saveAsync();
    }

    public void deleteWithChildren(DataStoreEntry... entries) {
        var toDelete = Arrays.stream(entries)
                .flatMap(entry -> {
                    var c = getDeepStoreChildren(entry);
                    c.add(entry);
                    return c.stream();
                })
                .toList();

        toDelete.forEach(entry -> entry.finalizeEntry());
        this.storeEntries.removeAll(toDelete);
        this.listeners.forEach(l -> l.onStoreRemove(toDelete.toArray(DataStoreEntry[]::new)));
        refreshValidities(false);
        saveAsync();
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

        var syntheticParent = getSyntheticParent(e);
        if (syntheticParent.isPresent()) {
            addStoreEntryIfNotPresent(syntheticParent.get());
        }

        var displayParent = syntheticParent.or(() -> getDisplayParent(e));
        if (displayParent.isPresent()) {
            displayParent.get().setExpanded(true);
            e.setCategoryUuid(displayParent.get().getCategoryUuid());
        }

        e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
        this.storeEntries.add(e);
        displayParent.ifPresent(p -> {
            p.setChildrenCache(null);
        });
        saveAsync();

        this.listeners.forEach(l -> l.onStoreAdd(e));
        e.initializeEntry();
        refreshValidities(true);
        return e;
    }

    public void addStoreEntriesIfNotPresent(@NonNull DataStoreEntry... es) {
        for (DataStoreEntry e : es) {
            if (storeEntries.contains(e) || getStoreEntryIfPresent(e.getStore()).isPresent()) {
                return;
            }

            var syntheticParent = getSyntheticParent(e);
            if (syntheticParent.isPresent()) {
                addStoreEntryIfNotPresent(syntheticParent.get());
            }

            var displayParent = syntheticParent.or(() -> getDisplayParent(e));
            if (displayParent.isPresent()) {
                displayParent.get().setExpanded(true);
            }

            e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
            this.storeEntries.add(e);
            displayParent.ifPresent(p -> {
                p.setChildrenCache(null);
            });
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

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        store.finalizeEntry();
        this.storeEntries.remove(store);
        getDisplayParent(store).ifPresent(p -> p.setChildrenCache(null));
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

    // Get operations

    public boolean isRootEntry(DataStoreEntry entry) {
        var noParent = DataStorage.get().getDisplayParent(entry).isEmpty();
        var diffParentCategory = DataStorage.get()
                .getDisplayParent(entry)
                .map(p -> !p.getCategoryUuid().equals(entry.getCategoryUuid()))
                .orElse(false);
        var loop = isParentLoop(entry);
        return noParent || diffParentCategory || loop;
    }

    private boolean isParentLoop(DataStoreEntry entry) {
        var es = new HashSet<DataStoreEntry>();

        DataStoreEntry current = entry;
        while ((current = getDisplayParent(current).orElse(null)) != null) {
            if (es.contains(current)) {
                return true;
            }

            es.add(current);
        }

        return false;
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

    public Optional<DataStoreEntry> getSyntheticParent(DataStoreEntry entry) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return Optional.empty();
        }

        try {
            var provider = entry.getProvider();
            return Optional.ofNullable(provider.getSyntheticParent(entry));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Optional<DataStoreEntry> getDisplayParent(DataStoreEntry entry) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return Optional.empty();
        }

        try {
            var provider = entry.getProvider();
            return Optional.ofNullable(provider.getDisplayParent(entry))
                    .filter(dataStoreEntry -> storeEntries.contains(dataStoreEntry));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Set<DataStoreEntry> getDeepStoreChildren(DataStoreEntry entry) {
        var set = new HashSet<DataStoreEntry>();
        getStoreChildren(entry).forEach(c -> {
            set.add(c);
            set.addAll(getDeepStoreChildren(c));
        });
        return set;
    }

    public Set<DataStoreEntry> getStoreChildren(DataStoreEntry entry) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return Set.of();
        }

        var entries = getStoreEntries();
        if (!entries.contains(entry)) {
            return Set.of();
        }

        if (entry.getChildrenCache() != null) {
            return entry.getChildrenCache();
        }

        var children = entries.stream()
                .filter(other -> {
                    if (other.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                        return false;
                    }

                    var parent = getDisplayParent(other);
                    return parent.isPresent() && parent.get().equals(entry) && !isParentLoop(entry);
                })
                .collect(Collectors.toSet());
        entry.setChildrenCache(children);
        return children;
    }

    public List<DataStore> getUsableStores() {
        return new ArrayList<>(getStoreEntries().stream()
                .filter(entry -> entry.getValidity().isUsable())
                .map(DataStoreEntry::getStore)
                .toList());
    }

    private List<DataStoreEntry> getHierarchy(DataStoreEntry entry) {
        var es = new ArrayList<DataStoreEntry>();

        DataStoreEntry current = entry;
        while ((current = getDisplayParent(current).orElse(null)) != null) {
            if (es.contains(current)) {
                break;
            }

            es.add(0, current);
        }

        return es;
    }

    public DataStoreId getId(DataStoreEntry entry) {
        return DataStoreId.create(getHierarchy(entry).stream()
                .filter(e -> !(e.getStore() instanceof LocalStore))
                .map(e -> e.getName().replaceAll(":", "_"))
                .toArray(String[]::new));
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStoreId id) {
        var current = getStoreEntryIfPresent(id.getNames().get(0));
        if (current.isPresent()) {
            for (int i = 1; i < id.getNames().size(); i++) {
                var children = getStoreChildren(current.get());
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
        return getStoreEntryIfPresent(store).orElseThrow(() -> new IllegalArgumentException("Store not found"));
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        return storeEntries.stream()
                .filter(n -> n.getStore() == store
                        || (n.getStore() != null
                                && Objects.equals(store.getClass(), n.getStore().getClass())
                                && store.equals(n.getStore())))
                .findFirst();
    }

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

    public Optional<DataStoreEntry> getStoreEntryIfPresent(UUID id) {
        return storeEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public DataStoreEntry getOrCreateNewSyntheticEntry(DataStoreEntry parent, String name, DataStore store) {
        var uuid = UuidHelper.generateFromObject(parent.getUuid(), name);
        var found = getStoreEntryIfPresent(uuid);
        if (found.isPresent()) {
            return found.get();
        }

        return DataStoreEntry.createNew(uuid, parent.getCategoryUuid(), name, store);
    }

    public DataStoreEntry getStoreEntry(UUID id) {
        return getStoreEntryIfPresent(id).orElseThrow();
    }

    public DataStoreEntry local() {
        return getStoreEntryIfPresent(LOCAL_ID).orElse(null);
    }
}
