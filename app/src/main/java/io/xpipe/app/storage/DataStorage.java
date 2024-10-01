package io.xpipe.app.storage;

import io.xpipe.app.comp.store.StoreSortMode;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedChildStore;
import io.xpipe.core.store.StorePath;
import io.xpipe.core.store.ValidationContext;
import io.xpipe.core.util.UuidHelper;

import javafx.util.Pair;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    protected final List<DataStoreCategory> storeCategories;

    protected final Map<DataStoreEntry, DataStoreEntry> storeEntries;

    @Getter
    protected final Set<DataStoreEntry> storeEntriesSet;

    protected final ReentrantLock busyIo = new ReentrantLock();

    @Getter
    private final List<StorageListener> listeners = new CopyOnWriteArrayList<>();

    private final Map<DataStoreEntry, DataStoreEntry> storeEntriesInProgress = new ConcurrentHashMap<>();

    @Getter
    protected boolean loaded;

    @Getter
    @Setter
    protected DataStoreCategory selectedCategory;

    private final Map<DataStore, DataStoreEntry> storeEntryMapCache =
            Collections.synchronizedMap(new IdentityHashMap<>());

    public DataStorage() {
        var prefsDir = AppPrefs.get().storageDirectory().getValue();
        this.dir = !Files.exists(prefsDir) || !Files.isDirectory(prefsDir) ? AppPrefs.DEFAULT_STORAGE_DIR : prefsDir;
        this.storeEntries = new ConcurrentHashMap<>();
        this.storeEntriesSet = storeEntries.keySet();
        this.storeCategories = new CopyOnWriteArrayList<>();
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
    }

    public static void reset() {
        if (INSTANCE == null) {
            return;
        }

        INSTANCE.dispose();
        INSTANCE = null;
    }

    public static DataStorage get() {
        return INSTANCE;
    }

    public abstract String getVaultKey();

    public DataStoreCategory getDefaultConnectionsCategory() {
        return getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllConnectionsCategory() {
        return getStoreCategoryIfPresent(ALL_CONNECTIONS_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllScriptsCategory() {
        return getStoreCategoryIfPresent(ALL_SCRIPTS_CATEGORY_UUID).orElseThrow();
    }

    public void forceRewrite() {
        TrackEvent.info("Starting forced storage rewrite");
        getStoreEntries().forEach(dataStoreEntry -> {
            dataStoreEntry.reassignStore();
        });
        TrackEvent.info("Finished forced storage rewrite");
    }

    private void dispose() {
        save(true);
        var finalizing = false;
        for (DataStoreEntry entry : getStoreEntries()) {
            // Prevent blocking of shutdown
            if (entry.finalizeEntryAsync()) {
                finalizing = true;
            }
        }
        if (finalizing) {
            ThreadHelper.sleep(1000);
        }
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
            var cat = DataStoreCategory.createNew(
                    ALL_SCRIPTS_CATEGORY_UUID, PREDEFINED_SCRIPTS_CATEGORY_UUID, "Predefined");
            cat.setDirectory(categoriesDir.resolve(PREDEFINED_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }

        if (getStoreCategoryIfPresent(CUSTOM_SCRIPTS_CATEGORY_UUID).isEmpty()) {
            var cat = DataStoreCategory.createNew(ALL_SCRIPTS_CATEGORY_UUID, CUSTOM_SCRIPTS_CATEGORY_UUID, "Custom");
            cat.setDirectory(categoriesDir.resolve(CUSTOM_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }

        if (getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).isEmpty()) {
            storeCategories.add(new DataStoreCategory(
                    categoriesDir.resolve(DEFAULT_CATEGORY_UUID.toString()),
                    DEFAULT_CATEGORY_UUID,
                    "Default",
                    Instant.now(),
                    Instant.now(),
                    null,
                    true,
                    ALL_CONNECTIONS_CATEGORY_UUID,
                    StoreSortMode.getDefault(),
                    false,
                    true));
        }

        storeCategories.forEach(dataStoreCategory -> {
            if (dataStoreCategory.getParentCategory() != null
                    && getStoreCategoryIfPresent(dataStoreCategory.getParentCategory())
                            .isEmpty()) {
                dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
            } else if (dataStoreCategory.getParentCategory() == null
                    && !dataStoreCategory.getUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID)
                    && !dataStoreCategory.getUuid().equals(ALL_SCRIPTS_CATEGORY_UUID)) {
                dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
            }
        });
    }

    protected Path getStoresDir() {
        return dir.resolve("stores");
    }

    public Path getDataDir() {
        return dir.resolve("data");
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
        // Technically the load and save operations also return instantly if locked, but let's not even create new
        // threads here
        if (busyIo.isLocked()) {
            return;
        }

        ThreadHelper.runAsync(() -> {
            save(false);
        });
    }

    public abstract void save(boolean dispose);

    public abstract boolean supportsSharing();

    public boolean shouldSync(DataStoreCategory category) {
        if (!category.canShare()) {
            return false;
        }

        DataStoreCategory c = category;
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

    public boolean shouldSync(DataStoreEntry entry) {
        if (!shouldSync(DataStorage.get()
                .getStoreCategoryIfPresent(entry.getCategoryUuid())
                .orElseThrow())) {
            return false;
        }

        DataStoreEntry c = entry;
        do {
            // We can't check for sharing of invalid entries
            if (!c.getValidity().isUsable()) {
                return false;
            }

            if (c.getStore() instanceof LocalStore && entry.getProvider().isShareableFromLocalMachine()) {
                return true;
            }

            if (!c.getProvider().isShareable(c)) {
                return false;
            }
        } while ((c = DataStorage.get().getDefaultDisplayParent(c).orElse(null)) != null);
        return true;
    }

    protected void refreshEntries() {
        storeEntries.keySet().forEach(dataStoreEntry -> {
            dataStoreEntry.refreshStore();
        });
    }

    public void updateEntry(DataStoreEntry entry, DataStoreEntry newEntry) {
        var state = entry.getStorePersistentState();
        var nState = newEntry.getStorePersistentState();
        if (state != null && nState != null) {
            var updatedState = state.mergeCopy(nState);
            newEntry.setStorePersistentState(updatedState);
        }

        var oldParent = DataStorage.get().getDefaultDisplayParent(entry);
        var newParent = DataStorage.get().getDefaultDisplayParent(newEntry);
        var sameParent = Objects.equals(oldParent, newParent);

        entry.finalizeEntry();

        var children = getDeepStoreChildren(entry);
        if (!sameParent) {
            var toRemove = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
            listeners.forEach(storageListener -> storageListener.onStoreRemove(toRemove));
        }

        entry.applyChanges(newEntry);
        entry.initializeEntry();

        if (!sameParent) {
            if (oldParent.isPresent()) {
                oldParent.get().setChildrenCache(null);
            }
            if (newParent.isPresent()) {
                newParent.get().setChildrenCache(null);
                newParent.get().setExpanded(true);
            }
            var toAdd = Stream.concat(Stream.of(entry), children.stream()).toArray(DataStoreEntry[]::new);
            listeners.forEach(storageListener -> storageListener.onStoreAdd(toAdd));
        }
        refreshEntries();
        saveAsync();
    }

    public void syncCategory(DataStoreCategory category, boolean share) {
        category.setSync(share);

        DataStoreCategory p = category;
        if (share) {
            while ((p = DataStorage.get()
                            .getStoreCategoryIfPresent(p.getParentCategory())
                            .orElse(null))
                    != null) {
                p.setSync(true);
            }
        }

        // Update git remote if needed
        DataStorage.get().saveAsync();
    }

    public void updateCategory(DataStoreEntry entry, DataStoreCategory newCategory) {
        if (getStoreCategoryIfPresent(entry.getUuid())
                .map(category -> category.equals(newCategory))
                .orElse(false)) {
            return;
        }

        entry.setCategoryUuid(newCategory.getUuid());
        var children = getDeepStoreChildren(entry);
        children.forEach(child -> child.setCategoryUuid(newCategory.getUuid()));
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
        saveAsync();
    }

    public void setOrder(DataStoreEntry entry, DataStoreEntry.Order order) {
        entry.setExplicitOrder(order);
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
    }

    @SneakyThrows
    public boolean refreshChildren(DataStoreEntry e, ValidationContext<?> context) {
        return refreshChildren(e, context, false);
    }

    @SuppressWarnings("unchecked")
    public <T extends ValidationContext<?>> boolean refreshChildren(DataStoreEntry e, T context, boolean throwOnFail)
            throws Exception {
        if (!(e.getStore() instanceof FixedHierarchyStore<?> h)) {
            return false;
        }

        e.incrementBusyCounter();
        List<? extends DataStoreEntryRef<? extends FixedChildStore>> newChildren;
        var hadContext = context != null;
        try {
            if (context == null) {
                context = (T) h.createContext();
                if (context == null) {
                    return false;
                }
            }

            newChildren = ((FixedHierarchyStore<T>) h)
                    .listChildren(context).stream()
                            .filter(dataStoreEntryRef -> dataStoreEntryRef != null && dataStoreEntryRef.get() != null)
                            .toList();
        } catch (Exception ex) {
            if (throwOnFail) {
                throw ex;
            } else {
                ErrorEvent.fromThrowable(ex).handle();
                return false;
            }
        } finally {
            if (context != null && !hadContext) {
                context.close();
            }
            e.decrementBusyCounter();
        }

        var oldChildren = getStoreChildren(e);
        var toRemove = oldChildren.stream()
                .filter(oc -> oc.getStore() instanceof FixedChildStore)
                .filter(oc -> {
                    if (!oc.getValidity().isUsable()) {
                        return true;
                    }

                    var oid = ((FixedChildStore) oc.getStore()).getFixedId();
                    if (oid.isEmpty()) {
                        return false;
                    }

                    return newChildren.stream()
                            .filter(nc -> nc.getStore().getFixedId().isPresent())
                            .noneMatch(nc -> {
                                return nc.getStore().getFixedId().getAsInt() == oid.getAsInt();
                            });
                })
                .toList();
        var toAdd = newChildren.stream()
                .filter(nc -> {
                    if (nc == null || nc.getStore() == null) {
                        return false;
                    }

                    var nid = nc.getStore().getFixedId();
                    // These can't be automatically generated
                    if (nid.isEmpty()) {
                        return false;
                    }

                    return oldChildren.stream()
                            .filter(oc -> oc.getStore() instanceof FixedChildStore)
                            .filter(oc -> oc.getValidity().isUsable())
                            .filter(oc -> ((FixedChildStore) oc.getStore())
                                    .getFixedId()
                                    .isPresent())
                            .noneMatch(oc -> {
                                return ((FixedChildStore) oc.getStore())
                                                .getFixedId()
                                                .getAsInt()
                                        == nid.getAsInt();
                            });
                })
                .toList();
        var toUpdate = oldChildren.stream()
                .filter(oc -> oc.getStore() instanceof FixedChildStore)
                .filter(oc -> oc.getValidity().isUsable())
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
        // Force instant to be later in case we are really quick
        ThreadHelper.sleep(1);
        toAdd.forEach(nc -> {
            // Update after parent entry
            nc.get().notifyUpdate(false, true);
        });

        if (h.removeLeftovers()) {
            deleteWithChildren(toRemove.toArray(DataStoreEntry[]::new));
        }
        addStoreEntriesIfNotPresent(toAdd.stream().map(DataStoreEntryRef::get).toArray(DataStoreEntry[]::new));
        toUpdate.forEach(pair -> {
            // Update state by merging
            if (pair.getKey().getStorePersistentState() != null
                    && pair.getValue().get().getStorePersistentState() != null) {
                var classMatch = pair.getKey()
                        .getStorePersistentState()
                        .getClass()
                        .equals(pair.getValue().get().getStorePersistentState().getClass());
                // Children classes might not be the same, the same goes for state classes
                // This can happen when there are multiple child classes and the ids got switched around
                if (classMatch) {
                    DataStore merged = ((FixedChildStore) pair.getKey().getStore())
                            .merge(pair.getValue().getStore().asNeeded());
                    if (merged != pair.getKey().getStore()) {
                        pair.getKey().setStoreInternal(merged, false);
                    }

                    var s = pair.getKey().getStorePersistentState();
                    var mergedState = s.mergeCopy(pair.getValue().get().getStorePersistentState());
                    pair.getKey().setStorePersistentState(mergedState);
                }
            }
        });
        refreshEntries();
        saveAsync();
        e.getProvider().onChildrenRefresh(e);
        toAdd.forEach(dataStoreEntryRef ->
                dataStoreEntryRef.get().getProvider().onParentRefresh(dataStoreEntryRef.getEntry()));
        toUpdate.forEach(dataStoreEntryRef ->
                dataStoreEntryRef.getKey().getProvider().onParentRefresh(dataStoreEntryRef.getKey()));
        return !newChildren.isEmpty();
    }

    public void deleteChildren(DataStoreEntry e) {
        var c = getDeepStoreChildren(e);
        if (c.isEmpty()) {
            return;
        }

        c.forEach(entry -> entry.finalizeEntry());
        this.storeEntriesSet.removeAll(c);
        this.listeners.forEach(l -> l.onStoreRemove(c.toArray(DataStoreEntry[]::new)));
        refreshEntries();
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
        if (toDelete.isEmpty()) {
            return;
        }

        toDelete.forEach(entry -> entry.finalizeEntry());
        toDelete.forEach(this.storeEntriesSet::remove);
        this.listeners.forEach(l -> l.onStoreRemove(toDelete.toArray(DataStoreEntry[]::new)));
        refreshEntries();
        saveAsync();
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
        e.refreshStore();
        return e;
    }

    public void addStoreEntriesIfNotPresent(@NonNull DataStoreEntry... es) {
        if (es.length == 0) {
            return;
        }

        var toAdd = Arrays.stream(es)
                .filter(e -> {
                    if (storeEntriesSet.contains(e)
                            || getStoreEntryIfPresent(e.getStore(), false).isPresent()) {
                        return false;
                    }
                    return true;
                })
                .toList();
        for (DataStoreEntry e : toAdd) {
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
        for (DataStoreEntry e : toAdd) {
            e.refreshStore();
        }
        this.listeners.forEach(l -> l.onStoreAdd(toAdd.toArray(DataStoreEntry[]::new)));
        for (DataStoreEntry e : toAdd) {
            e.initializeEntry();
        }
        saveAsync();
    }

    public DataStoreEntry addStoreIfNotPresent(@NonNull String name, DataStore store) {
        return addStoreIfNotPresent(null, name, store);
    }

    public DataStoreEntry addStoreIfNotPresent(DataStoreEntry related, @NonNull String name, DataStore store) {
        var f = getStoreEntryIfPresent(store, false);
        if (f.isPresent()) {
            return f.get();
        }

        var e = DataStoreEntry.createNew(
                UUID.randomUUID(),
                related != null ? related.getCategoryUuid() : selectedCategory.getUuid(),
                name,
                store);
        addStoreEntryIfNotPresent(e);
        return e;
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        store.finalizeEntry();
        this.storeEntries.remove(store);
        getDefaultDisplayParent(store).ifPresent(p -> p.setChildrenCache(null));
        this.listeners.forEach(l -> l.onStoreRemove(store));
        refreshEntries();
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
        boolean diffParentCategory = DataStorage.get()
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

    public DataColor getEffectiveColor(DataStoreEntry entry) {
        var root = getRootForEntry(entry);
        if (root.getColor() != null) {
            return root.getColor();
        }

        var cats = getCategoryParentHierarchy(
                getStoreCategoryIfPresent(entry.getCategoryUuid()).orElseThrow());
        for (DataStoreCategory cat : cats.reversed()) {
            if (cat.getColor() != null) {
                return cat.getColor();
            }
        }

        return null;
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

    public List<DataStoreCategory> getCategoryParentHierarchy(DataStoreCategory cat) {
        var es = new ArrayList<DataStoreCategory>();
        es.add(cat);

        DataStoreCategory current = cat;
        while ((current = getStoreCategoryIfPresent(current.getParentCategory()).orElse(null)) != null) {
            if (es.contains(current)) {
                break;
            }

            es.addFirst(current);
        }

        return es;
    }

    public List<DataStoreEntry> getStoreParentHierarchy(DataStoreEntry entry) {
        var es = new ArrayList<DataStoreEntry>();
        es.add(entry);

        DataStoreEntry current = entry;
        while ((current = getDefaultDisplayParent(current).orElse(null)) != null) {
            if (es.contains(current)) {
                break;
            }

            es.addFirst(current);
        }

        return es;
    }

    public StorePath getStorePath(DataStoreEntry entry) {
        return StorePath.create(getStoreParentHierarchy(entry).stream()
                .filter(e -> (entry.getStore() instanceof LocalStore) || !(e.getStore() instanceof LocalStore))
                .map(e -> e.getName().toLowerCase().replaceAll("/", "_"))
                .toArray(String[]::new));
    }

    public StorePath getStorePath(DataStoreCategory entry) {
        return StorePath.create(getCategoryParentHierarchy(entry).stream()
                .map(e -> e.getName().toLowerCase().replaceAll("/", "_"))
                .toArray(String[]::new));
    }

    public Optional<DataStoreEntry> getStoreEntryInProgressIfPresent(@NonNull DataStore store) {
        return storeEntriesInProgress.keySet().stream()
                .filter(n -> n.getStore() == store)
                .findFirst();
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store, boolean identityOnly) {
        if (identityOnly) {
            synchronized (storeEntryMapCache) {
                var found = storeEntryMapCache.get(store);
                if (found != null) {
                    return Optional.of(found);
                }
            }
        }

        var found = storeEntriesSet.stream()
                .filter(n -> n.getStore() == store
                        || (!identityOnly
                                && (n.getStore() != null
                                        && Objects.equals(
                                                store.getClass(), n.getStore().getClass())
                                        && store.equals(n.getStore()))))
                .findFirst();
        if (found.isPresent()) {
            if (identityOnly) {
                synchronized (storeEntryMapCache) {
                    storeEntryMapCache.put(store, found.get());
                }
            }
        }
        return found;
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

    public String getStoreEntryDisplayName(DataStoreEntry entry) {
        if (entry == null) {
            return "?";
        }

        if (!entry.getValidity().isUsable()) {
            return "?";
        }

        return entry.getProvider().displayName(entry);
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(UUID id) {
        return storeEntriesSet.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public Set<DataStoreEntry> getStoreEntries() {
        return storeEntriesSet;
    }

    public DataStoreEntry getOrCreateNewSyntheticEntry(DataStoreEntry parent, String name, DataStore store) {
        var forStoreIdentity = getStoreEntryIfPresent(store, true);
        if (forStoreIdentity.isPresent()) {
            return forStoreIdentity.get();
        }

        var uuid = UuidHelper.generateFromObject(parent.getUuid(), name);
        var found = getStoreEntryIfPresent(uuid);
        if (found.isPresent()) {
            return found.get();
        }

        var forStore = getStoreEntryIfPresent(store, false);
        if (forStore.isPresent()) {
            return forStore.get();
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
