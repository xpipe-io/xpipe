package io.xpipe.app.storage;

import io.xpipe.app.comp.store.StoreSortMode;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.*;
import io.xpipe.core.util.UuidHelper;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
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

    private static DataStorage INSTANCE;
    protected final Path dir;

    @Getter
    protected boolean loaded;

    @Getter
    protected final List<DataStoreCategory> storeCategories;

    protected final Map<DataStoreEntry, DataStoreEntry> storeEntries;

    @Getter
    protected final Set<DataStoreEntry> storeEntriesSet;

    @Getter
    @Setter
    protected DataStoreCategory selectedCategory;

    @Getter
    private final List<StorageListener> listeners = new CopyOnWriteArrayList<>();

    private final Map<DataStoreEntry, DataStoreEntry> storeEntriesInProgress = new ConcurrentHashMap<>();

    protected final ReentrantLock busyIo = new ReentrantLock();

    public DataStorage() {
        var prefsDir = AppPrefs.get().storageDirectory().getValue();
        this.dir = !Files.exists(prefsDir) || !Files.isDirectory(prefsDir) ? AppPrefs.DEFAULT_STORAGE_DIR : prefsDir;
        this.storeEntries = new ConcurrentHashMap<>();
        this.storeEntriesSet = storeEntries.keySet();
        this.storeCategories = new CopyOnWriteArrayList<>();
    }

    public abstract String getVaultKey();

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

    public void forceRewrite() {
        getStoreEntries().forEach(dataStoreEntry -> {
            dataStoreEntry.reassignStore();
        });
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.dispose();
        INSTANCE = null;
    }

    private synchronized void dispose() {
        onReset();
        save(true);
    }

    protected void setupBuiltinCategories() {
        var categoriesDir = getCategoriesDir();
        var allConnections = getStoreCategoryIfPresent(ALL_CONNECTIONS_CATEGORY_UUID);
        if (allConnections.isEmpty()) {
            var cat = DataStoreCategory.createNew(null, ALL_CONNECTIONS_CATEGORY_UUID, "All connections");
            cat.setDirectory(categoriesDir.resolve(ALL_CONNECTIONS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        } else {
            allConnections.get().setParentCategory(null);
        }

        var allScripts = getStoreCategoryIfPresent(ALL_SCRIPTS_CATEGORY_UUID);
        if (allScripts.isEmpty()) {
            var cat = DataStoreCategory.createNew(null, ALL_SCRIPTS_CATEGORY_UUID, "All scripts");
            cat.setDirectory(categoriesDir.resolve(ALL_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        } else {
            allScripts.get().setParentCategory(null);
        }

        if (getStoreCategoryIfPresent(PREDEFINED_SCRIPTS_CATEGORY_UUID).isEmpty()) {
            var cat = DataStoreCategory.createNew(ALL_SCRIPTS_CATEGORY_UUID, PREDEFINED_SCRIPTS_CATEGORY_UUID, "Predefined");
            cat.setDirectory(categoriesDir.resolve(PREDEFINED_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }

        if (getStoreCategoryIfPresent(CUSTOM_SCRIPTS_CATEGORY_UUID).isEmpty()) {
            var cat = DataStoreCategory.createNew(ALL_SCRIPTS_CATEGORY_UUID, CUSTOM_SCRIPTS_CATEGORY_UUID, "Custom");
            cat.setDirectory(categoriesDir.resolve(CUSTOM_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }

        if (getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).isEmpty()) {
            storeCategories.add(
                    new DataStoreCategory(categoriesDir.resolve(DEFAULT_CATEGORY_UUID.toString()), DEFAULT_CATEGORY_UUID, "Default", Instant.now(),
                            Instant.now(), true, ALL_CONNECTIONS_CATEGORY_UUID, StoreSortMode.ALPHABETICAL_ASC, false));
        }

        storeCategories.forEach(dataStoreCategory -> {
            if (dataStoreCategory.getParentCategory() != null && getStoreCategoryIfPresent(dataStoreCategory.getParentCategory()).isEmpty()) {
                dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
            } else if (dataStoreCategory.getParentCategory() == null && !dataStoreCategory.getUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID) &&
                    !dataStoreCategory.getUuid().equals(ALL_SCRIPTS_CATEGORY_UUID)) {
                dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
            }
        });
    }

    protected void onReset() {}

    public static DataStorage get() {
        return INSTANCE;
    }

    protected Path getStoresDir() {
        return dir.resolve("stores");
    }

    public Path getDataDir() {
        return dir.resolve("data");
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
        // If we are already loading or saving, don't queue up another operation.
        // This could otherwise lead to thread starvation with virtual threads
        // Technically the load and save operations also return instantly if locked, but let's not even create new threads here
        if (busyIo.isLocked()) {
            return;
        }

        ThreadHelper.runAsync(() -> {
            save(false);
        });
    }

    public abstract void save(boolean dispose);

    public abstract boolean supportsSharing();
    public boolean shouldShare(DataStoreCategory entry) {
        if (!entry.canShare()) {
            return false;
        }

        DataStoreCategory c = entry;
        do {
            if (!c.shouldShareChildren()) {
                return false;
            }
        } while ((c = DataStorage.get()
                .getStoreCategoryIfPresent(c.getParentCategory())
                .orElse(null))
                != null);
        return true;
    }

    public boolean shouldShare(DataStoreEntry entry) {
        if (!shouldShare(DataStorage.get()
                .getStoreCategoryIfPresent(entry.getCategoryUuid())
                .orElseThrow())) {
            return false;
        }

        DataStoreEntry c = entry;
        do {
            if (c.getStore() instanceof LocalStore && entry.getProvider().isShareableFromLocalMachine()) {
                return true;
            }

            if (!c.getProvider().isShareable(c)) {
                return false;
            }
        } while ((c = DataStorage.get().getDefaultDisplayParent(c).orElse(null)) != null);
        return true;
    }

    protected void refreshValidities(boolean makeValid) {
        var changed = new AtomicBoolean(false);
        do {
            changed.set(false);
            storeEntries.keySet().forEach(dataStoreEntry -> {
                if (makeValid ? dataStoreEntry.tryMakeValid() : dataStoreEntry.tryMakeInvalid()) {
                    changed.set(true);
                }
            });
        } while (changed.get());
    }

    public void updateEntry(DataStoreEntry entry, DataStoreEntry newEntry) {
        var oldParent = DataStorage.get().getDefaultDisplayParent(entry);
        var newParent = DataStorage.get().getDefaultDisplayParent(newEntry);
        var sameParent = Objects.equals(oldParent, newParent);

        newEntry.finalizeEntry();

        var children = getDeepStoreChildren(entry);
        if (!sameParent) {
            var toRemove = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
            listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));
        }

        entry.applyChanges(newEntry);
        entry.initializeEntry();

        if (!sameParent) {
            var toAdd = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
            listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
        }
        refreshValidities(true);

        saveAsync();
    }

    public void updateCategory(DataStoreEntry entry, DataStoreCategory newCategory) {
        if (getStoreCategoryIfPresent(entry.getUuid()).map(category -> category.equals(newCategory)).orElse(false)) {
            return;
        }

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
                .filter(other -> e.equals(getDefaultDisplayParent(other).orElse(null)))
                .toList();
        var toRemove = oldChildren.stream()
                .filter(oc -> {
                    var oid = ((FixedChildStore) oc.getStore()).getFixedId();
                    if (oid.isEmpty()) {
                        return false;
                    }

                    return newChildren.stream().filter(nc -> nc.getStore().getFixedId().isPresent()).noneMatch(nc -> {
                        return nc.getStore().getFixedId().getAsInt() == oid.getAsInt();
                    });
                })
                .toList();
        var toAdd = newChildren.stream()
                .filter(nc -> {
                    var nid = nc.getStore().getFixedId();
                    // These can't be automatically generated
                    if (nid.isEmpty()) {
                        return false;
                    }

                    return oldChildren.stream().filter(oc -> ((FixedChildStore) oc.getStore()).getFixedId().isPresent()).noneMatch(oc -> {
                        return ((FixedChildStore) oc.getStore()).getFixedId().getAsInt() == nid.getAsInt();
                    });
                })
                .toList();
        var toUpdate = oldChildren.stream()
                .map(oc -> {
                    var oid = ((FixedChildStore) oc.getStore()).getFixedId();
                    if (oid.isEmpty()) {
                        return new Pair<DataStoreEntry, DataStoreEntryRef<? extends FixedChildStore>>(oc, null);
                    }

                    var found = newChildren.stream()
                            .filter(nc -> nc.getStore().getFixedId().isPresent())
                            .filter(nc -> nc.getStore().getFixedId().getAsInt() == oid.getAsInt())
                            .findFirst()
                            .orElse(null);
                    return new Pair<DataStoreEntry, DataStoreEntryRef<? extends FixedChildStore>>(oc, found);
                })
                .filter(en -> en.getValue() != null)
                .toList();

        if (!newChildren.isEmpty()) {
            e.setExpanded(true);
        }

        deleteWithChildren(toRemove.toArray(DataStoreEntry[]::new));
        addStoreEntriesIfNotPresent(toAdd.stream().map(DataStoreEntryRef::get).toArray(DataStoreEntry[]::new));
        toUpdate.forEach(pair -> {
            pair.getKey().setStoreInternal(pair.getValue().getStore(), false);

            // Update state by merging
            if (pair.getKey().getStorePersistentState() != null && pair.getValue().get().getStorePersistentState() != null) {
                var mergedState = pair.getKey().getStorePersistentState().deepCopy();
                mergedState.merge(pair.getValue().get().getStorePersistentState());
                pair.getKey().setStorePersistentState(mergedState);
            }
        });
        saveAsync();
        return !newChildren.isEmpty();
    }

    public void deleteChildren(DataStoreEntry e) {
        var c = getDeepStoreChildren(e);
        c.forEach(entry -> entry.finalizeEntry());
        this.storeEntriesSet.removeAll(c);
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
        this.storeEntriesSet.removeAll(toDelete);
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

    public void addStoreEntryInProgress(@NonNull DataStoreEntry e) {
        this.storeEntriesInProgress.put(e, e);
    }

    public void removeStoreEntryInProgress(@NonNull DataStoreEntry e) {
        this.storeEntriesInProgress.remove(e);
    }

    public DataStoreEntry addStoreEntryIfNotPresent(@NonNull DataStoreEntry e) {
        var found = storeEntries.get(e);
        if (found != null) {
            return found;
        }

        var byId = getStoreEntryIfPresent(e.getUuid()).orElse(null);
        if (byId != null) {
            return byId;
        }

        if (getStoreCategoryIfPresent(e.getCategoryUuid()).isEmpty()) {
            e.setCategoryUuid(DEFAULT_CATEGORY_UUID);
        }

        var syntheticParent = getSyntheticParent(e);
        if (syntheticParent.isPresent()) {
            addStoreEntryIfNotPresent(syntheticParent.get());
        }

        var displayParent = syntheticParent.or(() -> getDefaultDisplayParent(e));
        if (displayParent.isPresent()) {
            displayParent.get().setExpanded(true);
            e.setCategoryUuid(displayParent.get().getCategoryUuid());
        }

        e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
        this.storeEntries.put(e, e);
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
            if (storeEntriesSet.contains(e) || getStoreEntryIfPresent(e.getStore()).isPresent()) {
                return;
            }

            var syntheticParent = getSyntheticParent(e);
            if (syntheticParent.isPresent()) {
                addStoreEntryIfNotPresent(syntheticParent.get());
            }

            var displayParent = syntheticParent.or(() -> getDefaultDisplayParent(e));
            if (displayParent.isPresent()) {
                displayParent.get().setExpanded(true);
                e.setCategoryUuid(displayParent.get().getCategoryUuid());
            }

            e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
            this.storeEntries.put(e, e);
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
        return addStoreIfNotPresent(null, name, store);
    }

    public DataStoreEntry addStoreIfNotPresent(DataStoreEntry related, @NonNull String name, DataStore store) {
        var f = getStoreEntryIfPresent(store);
        if (f.isPresent()) {
            return f.get();
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), related != null ? related.getCategoryUuid() : selectedCategory.getUuid(), name, store);
        addStoreEntryIfNotPresent(e);
        return e;
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        store.finalizeEntry();
        this.storeEntries.remove(store);
        getDefaultDisplayParent(store).ifPresent(p -> p.setChildrenCache(null));
        this.listeners.forEach(l -> l.onStoreRemove(store));
        refreshValidities(false);
        saveAsync();
    }

    public void deleteStoreCategory(@NonNull DataStoreCategory cat) {
        if (cat.getParentCategory() == null) {
            return;
        }

        if (cat.getUuid().equals(DEFAULT_CATEGORY_UUID) || cat.getUuid().equals(PREDEFINED_SCRIPTS_CATEGORY_UUID)) {
            return;
        }

        storeEntriesSet.forEach(entry -> {
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
        var noParent = DataStorage.get().getDefaultDisplayParent(entry).isEmpty();
        var diffParentCategory = DataStorage.get()
                .getDefaultDisplayParent(entry)
                .map(p -> !p.getCategoryUuid().equals(entry.getCategoryUuid()))
                .orElse(false);
        var loop = isParentLoop(entry);
        return noParent || diffParentCategory || loop;
    }

    private boolean isParentLoop(DataStoreEntry entry) {
        var es = new HashSet<DataStoreEntry>();

        DataStoreEntry current = entry;
        while ((current = getDefaultDisplayParent(current).orElse(null)) != null) {
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
        while ((parent = getDefaultDisplayParent(current)).isPresent()) {
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

    public Optional<DataStoreEntry> getDefaultDisplayParent(DataStoreEntry entry) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return Optional.empty();
        }

        try {
            var provider = entry.getProvider();
            return Optional.ofNullable(provider.getDisplayParent(entry))
                    .filter(dataStoreEntry -> storeEntriesSet.contains(dataStoreEntry));
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

                    var parent = getDefaultDisplayParent(other);
                    return parent.isPresent() && parent.get().equals(entry) && !isParentLoop(entry);
                })
                .collect(Collectors.toSet());
        entry.setChildrenCache(children);
        return children;
    }

    public List<DataStoreEntry> getUsableEntries() {
        return new ArrayList<>(getStoreEntries().stream()
                .filter(entry -> entry.getValidity().isUsable())
                .toList());
    }

    private List<DataStoreEntry> getHierarchy(DataStoreEntry entry) {
        var es = new ArrayList<DataStoreEntry>();
        es.add(entry);

        DataStoreEntry current = entry;
        while ((current = getDefaultDisplayParent(current).orElse(null)) != null) {
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

    public Optional<DataStoreEntry> getStoreEntryInProgressIfPresent(@NonNull DataStore store) {
        return storeEntriesInProgress.keySet().stream()
                .filter(n -> n.getStore() == store)
                .findFirst();
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        return storeEntriesSet.stream()
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
        return storeEntriesSet.stream()
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

        if (!store.getValidity().isUsable()) {
            return "?";
        }

        return store.getProvider().browserDisplayName(store.getStore());
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(UUID id) {
        return storeEntriesSet.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public Set<DataStoreEntry> getStoreEntries() {
        return storeEntriesSet;
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
