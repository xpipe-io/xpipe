package io.xpipe.app.storage;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.FixedChildStore;
import io.xpipe.app.ext.FixedHierarchyStore;
import io.xpipe.app.ext.GroupStore;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.ext.NameableStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.StorePath;

import javafx.util.Pair;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.crypto.SecretKey;

public abstract class DataStorage {

    public static final UUID ALL_CONNECTIONS_CATEGORY_UUID = UUID.fromString("bfb0b51a-e7a3-4ce4-8878-8d4cb5828d6c");
    public static final UUID ALL_SCRIPTS_CATEGORY_UUID = UUID.fromString("19024cf9-d192-41a9-88a6-a22694cf716a");
    public static final UUID PREDEFINED_SCRIPTS_CATEGORY_UUID = UUID.fromString("5faf1d71-0efc-4293-8b70-299406396973");
    public static final UUID CUSTOM_SCRIPTS_CATEGORY_UUID = UUID.fromString("d3496db5-b709-41f9-abc0-ee0a660fbab9");
    public static final UUID DEFAULT_CATEGORY_UUID = UUID.fromString("97458c07-75c0-4f9d-a06e-92d8cdf67c40");
    public static final UUID LOCAL_ID = UUID.fromString("f0ec68aa-63f5-405c-b178-9a4454556d6b");
    public static final UUID ALL_IDENTITIES_CATEGORY_UUID = UUID.fromString("23a5565d-b343-4ab2-abf4-48a5d12dda22");
    public static final UUID ALL_MACROS_CATEGORY_UUID = UUID.fromString("f65b769a-cec9-4f30-ad58-95fe68d79c2c");
    public static final UUID LOCAL_IDENTITIES_CATEGORY_UUID = UUID.fromString("e784de4e-abea-4cb8-a839-fc557cd23097");
    public static final UUID SYNCED_IDENTITIES_CATEGORY_UUID = UUID.fromString("69aa5040-28dc-451e-b4ff-1192ce5e1e3c");
    private static DataStorage INSTANCE;
    protected final Path dir;

    @Getter
    protected final List<DataStoreCategory> storeCategories;

    protected final Map<DataStoreEntry, DataStoreEntry> storeEntries;

    @Getter
    protected final Set<DataStoreEntry> storeEntriesSet;

    @Getter
    private final List<StorageListener> listeners = new CopyOnWriteArrayList<>();

    private final Map<DataStoreEntry, DataStoreEntry> storeEntriesInProgress = new ConcurrentHashMap<>();
    private final Map<DataStore, DataStoreEntry> identityStoreEntryMapCache = new IdentityHashMap<>();
    private final Map<DataStore, DataStoreEntry> storeEntryMapCache = new HashMap<>();

    @Getter
    @Setter
    protected DataStoreCategory selectedCategory;

    public DataStorage() {
        this.dir = getStorageDirectory();
        this.storeEntries = new ConcurrentHashMap<>();
        this.storeEntriesSet = storeEntries.keySet();
        this.storeCategories = new CopyOnWriteArrayList<>();
    }

    public static Path getStorageDirectory() {
        var dir = AppProperties.get().getDataDir().resolve("storage");
        return dir;
    }

    public static void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = AppProperties.get().isPersistData() ? new StandardStorage() : new ImpersistentStorage();
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

    public void generateCaches() {
        for (DataStoreEntry storeEntry : getStoreEntries()) {
            getStoreChildren(storeEntry);
        }
    }

    public abstract void reloadContent();

    public abstract SecretKey getVaultKey();

    public DataStoreCategory getDefaultConnectionsCategory() {
        return getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllConnectionsCategory() {
        return getStoreCategoryIfPresent(ALL_CONNECTIONS_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllScriptsCategory() {
        return getStoreCategoryIfPresent(ALL_SCRIPTS_CATEGORY_UUID).orElseThrow();
    }

    public DataStoreCategory getAllIdentitiesCategory() {
        return getStoreCategoryIfPresent(ALL_IDENTITIES_CATEGORY_UUID).orElseThrow();
    }

    @SuppressWarnings("unused")
    public DataStoreCategory getAllMacrosCategory() {
        return getStoreCategoryIfPresent(ALL_MACROS_CATEGORY_UUID).orElseThrow();
    }

    public void forceRewrite() {
        TrackEvent.info("Starting forced storage rewrite");
        getStoreEntries().forEach(dataStoreEntry -> {
            dataStoreEntry.reassignStoreNode();
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
            var cat =
                    DataStoreCategory.createNew(ALL_SCRIPTS_CATEGORY_UUID, PREDEFINED_SCRIPTS_CATEGORY_UUID, "Samples");
            cat.setDirectory(categoriesDir.resolve(PREDEFINED_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }

        if (getStoreCategoryIfPresent(CUSTOM_SCRIPTS_CATEGORY_UUID).isEmpty()) {
            var cat = DataStoreCategory.createNew(ALL_SCRIPTS_CATEGORY_UUID, CUSTOM_SCRIPTS_CATEGORY_UUID, "Custom");
            cat.setDirectory(categoriesDir.resolve(CUSTOM_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }

        var allIdentities = getStoreCategoryIfPresent(ALL_IDENTITIES_CATEGORY_UUID);
        if (allIdentities.isEmpty()) {
            var cat = DataStoreCategory.createNew(null, ALL_IDENTITIES_CATEGORY_UUID, "All identities");
            cat.setDirectory(categoriesDir.resolve(ALL_IDENTITIES_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        } else {
            allIdentities.get().setParentCategory(null);
        }

        var localIdentities = getStoreCategoryIfPresent(LOCAL_IDENTITIES_CATEGORY_UUID);
        if (localIdentities.isEmpty()) {
            var cat =
                    DataStoreCategory.createNew(ALL_IDENTITIES_CATEGORY_UUID, LOCAL_IDENTITIES_CATEGORY_UUID, "Local");
            cat.setDirectory(categoriesDir.resolve(LOCAL_IDENTITIES_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        } else {
            localIdentities.get().setParentCategory(ALL_IDENTITIES_CATEGORY_UUID);
        }

        //        var allMacros = getStoreCategoryIfPresent(ALL_MACROS_CATEGORY_UUID);
        //        if (allMacros.isEmpty()) {
        //            var cat = DataStoreCategory.createNew(null, ALL_MACROS_CATEGORY_UUID, "All macros");
        //            cat.setDirectory(categoriesDir.resolve(ALL_MACROS_CATEGORY_UUID.toString()));
        //            storeCategories.add(cat);
        //        } else {
        //            allMacros.get().setParentCategory(null);
        //        }

        if (supportsSync()) {
            var sharedIdentities = getStoreCategoryIfPresent(SYNCED_IDENTITIES_CATEGORY_UUID);
            if (sharedIdentities.isEmpty()) {
                var cat = DataStoreCategory.createNew(
                        ALL_IDENTITIES_CATEGORY_UUID, SYNCED_IDENTITIES_CATEGORY_UUID, "Synced");
                cat.setDirectory(categoriesDir.resolve(SYNCED_IDENTITIES_CATEGORY_UUID.toString()));
                cat.setConfig(cat.getConfig().withSync(true));
                storeCategories.add(cat);
            } else {
                sharedIdentities.get().setParentCategory(ALL_IDENTITIES_CATEGORY_UUID);
            }
        }

        if (getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).isEmpty()) {
            storeCategories.add(new DataStoreCategory(
                    categoriesDir.resolve(DEFAULT_CATEGORY_UUID.toString()),
                    DEFAULT_CATEGORY_UUID,
                    "Default",
                    Instant.now(),
                    Instant.now(),
                    true,
                    ALL_CONNECTIONS_CATEGORY_UUID,
                    true,
                    DataStoreCategoryConfig.empty()));
        }

        storeCategories.forEach(dataStoreCategory -> {
            if (dataStoreCategory.getParentCategory() != null
                    && getStoreCategoryIfPresent(dataStoreCategory.getParentCategory())
                            .isEmpty()) {
                dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
            } else if (dataStoreCategory.getParentCategory() == null
                    && !dataStoreCategory.getUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID)
                    && !dataStoreCategory.getUuid().equals(ALL_SCRIPTS_CATEGORY_UUID)
                    && !dataStoreCategory.getUuid().equals(ALL_IDENTITIES_CATEGORY_UUID)) {
                dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
            }
        });
    }

    public Path getStorageDir() {
        return dir;
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

    public abstract void saveAsync();

    public abstract void save(boolean dispose);

    public abstract boolean supportsSync();

    public boolean shouldSync(DataStoreCategory category) {
        // Don't sync lone identities category
        if (category.getUuid().equals(SYNCED_IDENTITIES_CATEGORY_UUID)
                && storeCategories.stream()
                        .filter(dataStoreCategory ->
                                !dataStoreCategory.getUuid().equals(SYNCED_IDENTITIES_CATEGORY_UUID))
                        .noneMatch(dataStoreCategory -> shouldSync(dataStoreCategory))) {
            return false;
        }

        if (!category.canShare()) {
            return false;
        }

        var config = getEffectiveCategoryConfig(category);
        return Boolean.TRUE.equals(config.getSync());
    }

    public boolean shouldSync(DataStoreEntry entry) {
        if (!shouldSync(DataStorage.get()
                .getStoreCategoryIfPresent(entry.getCategoryUuid())
                .orElseThrow())) {
            return false;
        }

        DataStoreEntry c = entry;
        do {
            // We can't check for sharing of failed entries
            if (c.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                return false;
            }

            if (c.getStore() instanceof LocalStore && entry.getProvider().isShareableFromLocalMachine()) {
                return true;
            }

            try {
                if (!c.getProvider().isShareable(c)) {
                    return false;
                }
            } catch (Exception e) {
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

        var icon = entry.getIcon();
        if (icon != null && newEntry.getIcon() == null) {
            newEntry.setIcon(icon, true);
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

        if (entry.getStore() != null) {
            synchronized (identityStoreEntryMapCache) {
                identityStoreEntryMapCache.remove(entry.getStore());
            }
            synchronized (storeEntryMapCache) {
                storeEntryMapCache.remove(entry.getStore());
            }
        }

        var categoryChanged = !entry.getCategoryUuid().equals(newEntry.getCategoryUuid());

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

        if (categoryChanged) {
            listeners.forEach(storageListener -> storageListener.onEntryCategoryChange());
            listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
        }

        SecretManager.moveReferences(newEntry.getUuid(), entry.getUuid());

        refreshEntries();
        saveAsync();
    }

    public void updateCategory(DataStoreCategory category, DataStoreCategory newCategory) {
        category.setName(newCategory.getName());
        category.setParentCategory(newCategory.getParentCategory());
        updateCategoryConfig(category, newCategory.getConfig());
        saveAsync();
    }

    public void updateCategoryConfig(DataStoreCategory category, DataStoreCategoryConfig config) {
        if (category.setConfig(config)) {
            // Update git remote if needed
            DataStorage.get().saveAsync();
        }
    }

    public DataStoreCategory breakOutCategory(DataStoreEntry entry) {
        if (!(entry.getStore() instanceof FixedHierarchyStore) && !(entry.getStore() instanceof GroupStore<?>)) {
            return null;
        }

        var cat = getStoreCategory(entry);
        var breakOut = new DataStoreCategory(
                null,
                UUID.randomUUID(),
                entry.getName(),
                Instant.now(),
                Instant.now(),
                true,
                cat.getUuid(),
                true,
                DataStoreCategoryConfig.empty());
        addStoreCategory(breakOut);
        entry.setBreakOutCategory(breakOut);
        entry.setExpanded(true);

        var children = getDeepStoreChildren(entry);
        var childrenToKeep = new HashSet<DataStoreEntry>();
        children.forEach(c -> {
            if (c.getBreakOutCategory() != null) {
                childrenToKeep.addAll(getDeepStoreChildren(c));
                childrenToKeep.add(c);
            }
        });
        children.forEach(child -> {
            if (!childrenToKeep.contains(child)) {
                child.setCategoryUuid(breakOut.getUuid());
            }
        });
        entry.setCategoryUuid(breakOut.getUuid());

        var categoriesToMove = new ArrayList<DataStoreCategory>();
        children.forEach(child -> {
            if (child.getBreakOutCategory() != null) {
                var childBreakOut = getStoreCategoryIfPresent(child.getBreakOutCategory());
                if (childBreakOut.isPresent()
                        && childBreakOut.get().getParentCategory().equals(cat.getUuid())) {
                    categoriesToMove.add(childBreakOut.get());
                }
            }
        });
        categoriesToMove.forEach(toMove -> {
            toMove.setParentCategory(breakOut.getUuid());
            // The update mechanism does not support moves, so readd them
            listeners.forEach(storageListener -> storageListener.onCategoryRemove(toMove));
            listeners.forEach(storageListener -> storageListener.onCategoryAdd(toMove));
        });

        listeners.forEach(storageListener -> storageListener.onEntryCategoryChange());
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());

        saveAsync();
        return breakOut;
    }

    public void mergeBreakOutCategory(DataStoreEntry entry) {
        if (entry.getBreakOutCategory() == null) {
            return;
        }

        var breakOut = getStoreCategoryIfPresent(entry.getBreakOutCategory());
        if (breakOut.isEmpty()) {
            entry.setBreakOutCategory(null);
            return;
        }

        var parent = getDefaultDisplayParent(entry).or(() -> getSyntheticParent(entry));
        if (parent.isEmpty()) {
            deleteStoreCategory(breakOut.get(), false, false);
            return;
        }

        var moveCategories = new ArrayList<DataStoreCategory>();
        var children = getDeepStoreChildren(entry);

        var childrenToKeep = new HashSet<DataStoreEntry>();
        children.forEach(c -> {
            if (c.getBreakOutCategory() != null) {
                childrenToKeep.addAll(getDeepStoreChildren(c));
                childrenToKeep.add(c);
            }
        });

        children.forEach(child -> {
            if (childrenToKeep.contains(child)) {
                var cbo = getStoreCategoryIfPresent(child.getBreakOutCategory());
                if (cbo.isPresent()
                        && cbo.get().getParentCategory().equals(breakOut.get().getUuid())) {
                    moveCategories.add(cbo.get());
                }
                return;
            }

            child.setCategoryUuid(parent.get().getCategoryUuid());
        });
        moveCategories.forEach(toMove -> {
            toMove.setParentCategory(parent.get().getCategoryUuid());
            // The update mechanism does not support moves, so readd them
            listeners.forEach(storageListener -> storageListener.onCategoryRemove(toMove));
            listeners.forEach(storageListener -> storageListener.onCategoryAdd(toMove));
        });
        entry.setCategoryUuid(parent.get().getCategoryUuid());

        listeners.forEach(storageListener -> storageListener.onEntryCategoryChange());
        deleteStoreCategory(breakOut.get(), false, false);
        entry.setBreakOutCategory(null);
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
        saveAsync();
    }

    public void moveEntryToCategory(DataStoreEntry entry, DataStoreCategory newCategory) {
        if (newCategory.getUuid().equals(entry.getCategoryUuid())) {
            return;
        }

        var oldCat = getStoreCategoryIfPresent(entry.getCategoryUuid()).orElse(getDefaultConnectionsCategory());
        entry.setCategoryUuid(newCategory.getUuid());
        var children = getDeepStoreChildren(entry);
        children.forEach(child -> {
            if (!child.getCategoryUuid().equals(oldCat.getUuid())) {
                return;
            }

            child.setCategoryUuid(newCategory.getUuid());
        });
        listeners.forEach(storageListener -> storageListener.onEntryCategoryChange());
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
        saveAsync();
    }


    public void moveCategoryToParent(DataStoreCategory cat, DataStoreCategory newParent) {
        if (newParent.getUuid().equals(cat.getUuid())) {
            return;
        }

        if (cat.getParentCategory() == null) {
            return;
        }

        cat.setParentCategory(newParent.getUuid());
        listeners.forEach(storageListener -> storageListener.onEntryCategoryChange());
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
        saveAsync();
    }

    public void setOrderIndex(DataStoreEntry entry, int index) {
        entry.setOrderIndex(index);
        listeners.forEach(storageListener -> storageListener.onStoreListUpdate());
        saveAsync();
    }

    @SneakyThrows
    public boolean refreshChildren(DataStoreEntry e) {
        return refreshChildren(e, false);
    }

    public boolean refreshChildrenOrThrow(DataStoreEntry e) throws Exception {
        return refreshChildren(e, true);
    }

    public boolean refreshChildren(DataStoreEntry e, boolean throwOnFail) throws Exception {
        if (!(e.getStore() instanceof FixedHierarchyStore h)) {
            return false;
        }

        e.incrementBusyCounter();
        List<? extends DataStoreEntryRef<? extends FixedChildStore>> newChildren;
        try {
            List<? extends DataStoreEntryRef<? extends FixedChildStore>> l = h.listChildren();
            if (l != null) {
                newChildren = l.stream()
                        .filter(dataStoreEntryRef -> dataStoreEntryRef != null && dataStoreEntryRef.get() != null)
                        .toList();
                e.getProvider().onChildrenRefresh(e);
            } else {
                newChildren = null;
            }
        } catch (Exception ex) {
            if (throwOnFail) {
                throw ex;
            } else {
                ErrorEventFactory.fromThrowable(ex).handle();
                return false;
            }
        } finally {
            e.decrementBusyCounter();
        }

        if (newChildren == null) {
            return false;
        }

        var oldChildren = getStoreChildren(e);
        var toRemove = oldChildren.stream()
                .filter(oc -> {
                    var oid = getFixedChildId(oc);
                    if (oid.isEmpty()) {
                        return false;
                    }

                    return newChildren.stream()
                            .filter(nc -> getFixedChildId(nc.get()).isPresent())
                            .noneMatch(nc -> {
                                return getFixedChildId(nc.get()).getAsInt() == oid.getAsInt();
                            });
                })
                .toList();
        var toAdd = newChildren.stream()
                .filter(nc -> {
                    var nid = getFixedChildId(nc.get());
                    // These can't be automatically generated
                    if (nid.isEmpty()) {
                        return false;
                    }

                    return oldChildren.stream()
                            .filter(oc -> oc.getStore() instanceof FixedChildStore)
                            .filter(oc -> getFixedChildId(oc).isPresent())
                            .noneMatch(oc -> {
                                return getFixedChildId(oc).getAsInt() == nid.getAsInt();
                            });
                })
                .toList();
        var toUpdate = new ArrayList<>(oldChildren.stream()
                .map(oc -> {
                    var oid = getFixedChildId(oc);
                    if (oid.isEmpty()) {
                        return new Pair<DataStoreEntry, DataStoreEntryRef<? extends FixedChildStore>>(oc, null);
                    }

                    var found = newChildren.stream()
                            .filter(nc -> getFixedChildId(nc.get()).isPresent())
                            .filter(nc -> getFixedChildId(nc.get()).getAsInt() == oid.getAsInt())
                            .findFirst()
                            .orElse(null);
                    return new Pair<DataStoreEntry, DataStoreEntryRef<? extends FixedChildStore>>(oc, found);
                })
                .filter(en -> en.getValue() != null)
                .toList());

        toUpdate.removeIf(pair -> {
            // Children classes might not be the same, the same goes for state classes
            // This can happen when there are multiple child classes and the ids got switched around
            var storeClassMatch = pair.getKey()
                    .getStore()
                    .getClass()
                    .equals(pair.getValue().get().getStore().getClass());
            if (!storeClassMatch) {
                return true;
            }

            DataStore merged = ((FixedChildStore) pair.getKey().getStore())
                    .merge(pair.getValue().getStore().asNeeded());
            var mergedStoreChanged = pair.getKey().getStore() != merged;

            var nameChanged =
                    shouldUpdateChildrenStoreName(pair.getKey(), pair.getValue().get());

            if (pair.getKey().getStorePersistentState() == null
                    || pair.getValue().get().getStorePersistentState() == null) {
                return !mergedStoreChanged && !nameChanged;
            }

            var stateClassMatch = pair.getKey()
                    .getStorePersistentState()
                    .getClass()
                    .equals(pair.getValue().get().getStorePersistentState().getClass());
            if (!stateClassMatch) {
                return true;
            }

            var stateChange = !pair.getKey()
                    .getStorePersistentState()
                    .equals(pair.getValue().get().getStorePersistentState());
            return !mergedStoreChanged && !stateChange && !nameChanged;
        });

        if (toRemove.isEmpty() && toAdd.isEmpty() && toUpdate.isEmpty()) {
            return false;
        }

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
        if (e.getBreakOutCategory() != null) {
            toAdd.forEach(nc -> nc.get().setCategoryUuid(e.getBreakOutCategory()));
        }
        addStoreEntriesIfNotPresent(toAdd.stream().map(DataStoreEntryRef::get).toArray(DataStoreEntry[]::new));
        toUpdate.forEach(pair -> {
            if (shouldUpdateChildrenStoreName(pair.getKey(), pair.getValue().get())) {
                pair.getKey()
                        .setName(getNameableStoreName(pair.getValue().get())
                                .orElse(pair.getKey().getName()));
            }

            DataStore merged = ((FixedChildStore) pair.getKey().getStore())
                    .merge(pair.getValue().getStore().asNeeded());
            if (merged != null && !merged.equals(pair.getKey().getStore())) {
                pair.getKey().setStoreInternal(merged, false);
            }

            var s = pair.getKey().getStorePersistentState();
            var mergedState = s.mergeCopy(pair.getValue().get().getStorePersistentState());
            pair.getKey().setStorePersistentState(mergedState);

            if (pair.getKey().getOrderIndex() == 0 && pair.getValue().get().getOrderIndex() != 0) {
                pair.getKey().setOrderIndex(pair.getValue().get().getOrderIndex());
            }
        });
        refreshEntries();
        saveAsync();
        toAdd.forEach(
                dataStoreEntryRef -> dataStoreEntryRef.get().getProvider().onParentRefresh(dataStoreEntryRef.get()));
        toUpdate.forEach(dataStoreEntryRef ->
                dataStoreEntryRef.getKey().getProvider().onParentRefresh(dataStoreEntryRef.getKey()));
        return !newChildren.isEmpty();
    }

    private boolean shouldUpdateChildrenStoreName(DataStoreEntry o, DataStoreEntry n) {
        var oldName = getNameableStoreName(o);
        if (oldName.isEmpty()) {
            return false;
        }

        var isCustom = !o.getName().equals(oldName.get());
        if (isCustom) {
            return false;
        }

        var newName = getNameableStoreName(n);
        if (newName.isEmpty()) {
            return false;
        }
        return !o.getName().equals(newName.get());
    }

    private Optional<String> getNameableStoreName(DataStoreEntry o) {
        if (!(o.getStore() instanceof NameableStore nameable)) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(nameable.getName());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private OptionalInt getFixedChildId(DataStoreEntry entry) {
        if (!(entry.getStore() instanceof FixedChildStore f)) {
            return OptionalInt.empty();
        }

        try {
            return f.getFixedId();
        } catch (Throwable t) {
            return OptionalInt.empty();
        }
    }

    public void deleteWithChildren(DataStoreEntry... entries) {
        List<DataStoreEntry> toDelete = Arrays.stream(entries)
                .flatMap(entry -> {
                    var c = getDeepStoreChildren(entry);
                    c.add(entry);
                    return c.stream();
                })
                .toList();
        if (toDelete.isEmpty()) {
            return;
        }

        for (var td : toDelete) {
            td.finalizeEntry();
            this.storeEntriesSet.remove(td);
            synchronized (identityStoreEntryMapCache) {
                identityStoreEntryMapCache.remove(td.getStore());
            }
            synchronized (storeEntryMapCache) {
                storeEntryMapCache.remove(td.getStore());
            }
            var parent = getDefaultDisplayParent(td);
            parent.ifPresent(p -> p.setChildrenCache(null));
        }

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
            if (displayParent.isPresent()
                    && (displayParent.get().getBreakOutCategory() == null
                            || getStoreCategoryIfPresent(displayParent.get().getBreakOutCategory())
                                    .isEmpty())) {
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
        synchronized (identityStoreEntryMapCache) {
            identityStoreEntryMapCache.remove(store.getStore());
        }
        synchronized (storeEntryMapCache) {
            storeEntryMapCache.remove(store.getStore());
        }
        getDefaultDisplayParent(store).ifPresent(p -> p.setChildrenCache(null));
        this.listeners.forEach(l -> l.onStoreRemove(store));
        refreshEntries();
        saveAsync();
    }

    public boolean canDeleteStoreCategory(@NonNull DataStoreCategory cat) {
        if (cat.getParentCategory() == null) {
            return false;
        }

        if (cat.getUuid().equals(DEFAULT_CATEGORY_UUID)
                || cat.getUuid().equals(PREDEFINED_SCRIPTS_CATEGORY_UUID)
                || cat.getUuid().equals(LOCAL_IDENTITIES_CATEGORY_UUID)
                || cat.getUuid().equals(CUSTOM_SCRIPTS_CATEGORY_UUID)
                || cat.getUuid().equals(SYNCED_IDENTITIES_CATEGORY_UUID)) {
            return false;
        }

        return true;
    }

    public void deleteStoreCategory(@NonNull DataStoreCategory cat, boolean deleteChildren, boolean deleteEntries) {
        if (!canDeleteStoreCategory(cat)) {
            return;
        }

        var toDelete = new ArrayList<DataStoreCategory>();
        if (deleteChildren) {
            for (DataStoreCategory other : getStoreCategories()) {
                var hierarchy = getCategoryParentHierarchy(other);
                if (hierarchy.contains(cat)) {
                    toDelete.add(other);
                }
            }
        } else {
            toDelete.add(cat);
        }

        for (DataStoreCategory delCat : toDelete) {
            if (deleteEntries) {
                var toDeleteEntries = new ArrayList<DataStoreEntry>();
                for (DataStoreEntry entry : storeEntriesSet) {
                    if (getStoreCategory(entry).equals(delCat)) {
                        toDeleteEntries.add(entry);
                    }
                }
                deleteWithChildren(toDeleteEntries.toArray(DataStoreEntry[]::new));
            } else {
                storeEntriesSet.forEach(entry -> {
                    if (entry.getCategoryUuid().equals(delCat.getUuid())) {
                        entry.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }
                });
            }

            storeCategories.remove(delCat);
            this.listeners.forEach(l -> l.onCategoryRemove(delCat));
        }

        saveAsync();
    }

    // Get operations

    public boolean isRootEntry(DataStoreEntry entry, DataStoreCategory current) {
        if (entry.isPinToTop()) {
            return true;
        }

        var parent = getDefaultDisplayParent(entry);
        var noParent = parent.isEmpty();
        if (noParent) {
            return true;
        }

        var parentCat = getStoreCategoryIfPresent(parent.get().getCategoryUuid());
        if (parentCat.isEmpty()) {
            return true;
        }

        var parentCatHierarchy = getCategoryParentHierarchy(parentCat.get());
        var cat = getStoreCategoryIfPresent(entry.getCategoryUuid());
        if (cat.isEmpty()) {
            return true;
        }

        var catHierarchy = getCategoryParentHierarchy(cat.get());

        var currentContainsBoth = catHierarchy.contains(current) && parentCatHierarchy.contains(current);
        if (currentContainsBoth) {
            return false;
        }

        var diffParentCategoryHierarchy = !catHierarchy.contains(parentCat.get());
        if (diffParentCategoryHierarchy) {
            return true;
        }

        var subParent = catHierarchy.indexOf(current) > catHierarchy.indexOf(parentCat.get());
        if (subParent) {
            return true;
        }

        var loop = isParentLoop(entry);
        return loop;
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

    public boolean getEffectiveReadOnlyState(DataStoreEntry entry) {
        var cat = getStoreCategoryIfPresent(entry.getCategoryUuid());
        if (cat.isEmpty()) {
            return false;
        }

        var catConfig = getEffectiveCategoryConfig(cat.get());
        return catConfig.getFreezeConfigurations() != null ? catConfig.getFreezeConfigurations() : entry.isFreeze();
    }

    public DataStoreColor getEffectiveColor(DataStoreEntry entry) {
        var cat = getStoreCategoryIfPresent(entry.getCategoryUuid()).orElseThrow();
        var root = getRootForEntry(entry, cat);
        if (root.getColor() != null) {
            return root.getColor();
        }

        var catConfig = getEffectiveCategoryConfig(cat);
        return catConfig.getColor();
    }

    public DataStoreEntry getRootForEntry(DataStoreEntry entry, DataStoreCategory cat) {
        if (entry == null) {
            return null;
        }

        if (isRootEntry(entry, cat)) {
            return entry;
        }

        var current = entry;
        Optional<DataStoreEntry> parent;
        while ((parent = getDefaultDisplayParent(current)).isPresent()) {
            current = parent.get();
            if (isRootEntry(current, cat)) {
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
                    .filter(dataStoreEntry -> storeEntries.get(dataStoreEntry) != null);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public Set<DataStoreEntry> getDeepStoreChildren(DataStoreEntry entry) {
        var set = new HashSet<DataStoreEntry>();
        getDeepStoreChildren(entry, set);
        return set;
    }

    private void getDeepStoreChildren(DataStoreEntry entry, Set<DataStoreEntry> current) {
        getStoreChildren(entry).forEach(c -> {
            var added = current.add(c);
            // Guard against loop
            if (added) {
                getDeepStoreChildren(c, current);
            }
        });
    }

    public Set<DataStoreEntry> getStoreChildren(DataStoreEntry entry) {
        if (entry.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
            return Set.of();
        }

        if (entry.getChildrenCache() != null) {
            return entry.getChildrenCache();
        }

        var entries = getStoreEntries();
        if (!entries.contains(entry)) {
            return Set.of();
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
            synchronized (identityStoreEntryMapCache) {
                var found = identityStoreEntryMapCache.get(store);
                if (found != null) {
                    return Optional.of(found);
                }
            }
        } else {
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
                synchronized (identityStoreEntryMapCache) {
                    identityStoreEntryMapCache.put(store, found.get());
                }
            } else {
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

    public DataStoreCategory getStoreCategory(DataStoreEntry entry) {
        return getStoreCategoryIfPresent(entry.getCategoryUuid()).orElseThrow();
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
            return "Unknown";
        }

        if (!entry.getValidity().isUsable()) {
            return "Unknown";
        }

        return entry.getProvider().displayName(entry);
    }

    public DataStoreCategoryConfig getEffectiveCategoryConfig(DataStoreEntry entry) {
        var category = getStoreCategoryIfPresent(entry.getCategoryUuid());
        if (category.isEmpty()) {
            return DataStoreCategoryConfig.empty();
        }

        return getEffectiveCategoryConfig(category.get());
    }

    public DataStoreCategoryConfig getEffectiveCategoryConfig(DataStoreCategory category) {
        var hierarchy = getCategoryParentHierarchy(category);
        return DataStoreCategoryConfig.merge(hierarchy.stream()
                .map(dataStoreCategory -> dataStoreCategory.getConfig())
                .toList());
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(UUID id) {
        return storeEntriesSet.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public Set<DataStoreEntry> getStoreEntries() {
        return storeEntriesSet;
    }

    public DataStoreEntry getOrCreateNewSyntheticEntry(DataStoreEntry parent, String name, DataStore store) {
        var forStore = getStoreEntryIfPresent(store, false);
        if (forStore.isPresent()) {
            return forStore.get();
        }

        return DataStoreEntry.createNew(UUID.randomUUID(), parent.getCategoryUuid(), name, store);
    }

    public DataStoreEntry getStoreEntry(UUID id) {
        return getStoreEntryIfPresent(id).orElseThrow();
    }

    public DataStoreEntry local() {
        return getStoreEntryIfPresent(LOCAL_ID)
                .orElseThrow(() ->
                        new IllegalStateException("Missing local machine connection, restart is required to fix this"));
    }
}
