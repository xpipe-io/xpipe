package io.xpipe.app.storage;

import io.xpipe.app.core.AppCharsets;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.charsetter.Charsettable;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedHierarchyStore;
import lombok.Getter;
import lombok.NonNull;

import java.nio.file.Path;
import java.util.*;

public abstract class DataStorage {

    private static final String PERSIST_PROP = "io.xpipe.storage.persist";
    private static final String IMMUTABLE_PROP = "io.xpipe.storage.immutable";

    private static DataStorage INSTANCE;
    protected final Path dir;
    protected final List<DataSourceEntry> sourceEntries;
    protected final List<DataSourceCollection> sourceCollections;
    protected final List<DataStoreEntry> storeEntries;

    @Getter
    private final List<StorageListener> listeners = new ArrayList<>();

    private DataSourceEntry latest;

    public DataStorage() {
        this.dir = AppPrefs.get().storageDirectory().getValue();
        this.sourceEntries = new ArrayList<>();
        this.sourceCollections = new ArrayList<>();
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

    public synchronized DataSourceCollection getInternalCollection() {
        var found = sourceCollections.stream()
                .filter(o -> o.getName() != null && o.getName().equals("Internal"))
                .findAny();
        if (found.isPresent()) {
            return found.get();
        }

        var internalCollection = DataSourceCollection.createNew("Internal");
        internalCollection.setDirectory(getSourcesDir()
                .resolve("collections")
                .resolve(internalCollection.getUuid().toString()));
        this.sourceCollections.add(internalCollection);
        return internalCollection;
    }

    public synchronized void refreshChildren(DataStoreEntry e) {
        if (!(e.getStore() instanceof FixedHierarchyStore)) {
            return;
        }

        try {
            var newChildren = ((FixedHierarchyStore) e.getStore()).listChildren();
            deleteChildren(e, true);
            newChildren.forEach((key, value) -> {
                try {
                    addStoreEntry(key, value);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    public synchronized void deleteChildren(DataStoreEntry e, boolean  deep) {
        getStoreChildren(e,deep).forEach(entry -> {
            if (!entry.getConfiguration().isDeletable()) {
                return;
            }

            deleteStoreEntry(entry);
        });
    }

    public synchronized List<DataStoreEntry> getStoreChildren(DataStoreEntry entry,   boolean  deep) {
        var children = new ArrayList<>(getStoreEntries().stream().filter(other -> {
            if (!other.getState().isUsable()) {
                return false;
            }

            var parent = other
                    .getProvider()
                    .getParent(other.getStore());
            return entry.getStore().equals(parent);
        }).toList());

        if (deep) {
            for (DataStoreEntry dataStoreEntry : new ArrayList<>(children)) {
                children.addAll(getStoreChildren(dataStoreEntry, true));
            }
        }

        return children;
    }

    public synchronized String createUniqueSourceEntryName(DataSourceCollection col, DataSource<?> source) {
        var def = source.determineDefaultName();
        if (def.isPresent()) {
            return createUniqueSourceEntryName(col, def.get());
        }

        var storeDef = source.getStore().determineDefaultName();
        if (storeDef.isPresent()) {
            return createUniqueSourceEntryName(col, storeDef.get());
        }

        var typeName =
                switch (source.getType()) {
                    case TABLE -> AppI18n.get("table");
                    case STRUCTURE -> AppI18n.get("structure");
                    case TEXT -> AppI18n.get("text");
                    case RAW -> AppI18n.get("raw");
                    case COLLECTION -> AppI18n.get("collection");
                };
        return createUniqueSourceEntryName(col, typeName);
    }

    private synchronized String createUniqueStoreEntryName(String base) {
        if (DataStorage.get().getStoreEntryIfPresent(base).isEmpty()) {
            return base;
        }

        int counter = 1;
        while (true) {
            var name = base + counter;
            if (DataStorage.get().getStoreEntryIfPresent(name).isEmpty()) {
                return name;
            }
            counter++;
        }
    }

    private synchronized String createUniqueSourceEntryName(DataSourceCollection col, String base) {
        base = DataSourceId.cleanString(base);
        var id = DataSourceId.create(col != null ? col.getName() : null, base);
        if (DataStorage.get().getDataSource(DataSourceReference.id(id)).isEmpty()) {
            return base;
        }

        int counter = 1;
        while (true) {
            id = DataSourceId.create(col != null ? col.getName() : null, base + counter);
            if (DataStorage.get().getDataSource(DataSourceReference.id(id)).isEmpty()) {
                return base + counter;
            }
            counter++;
        }
    }

    public synchronized DataSourceEntry getLatest() {
        return latest;
    }

    public synchronized void setLatest(DataSourceEntry latest) {
        this.latest = latest;
    }

    public abstract Path getInternalStreamPath(@NonNull UUID uuid);

    private void checkImmutable() {
        if (System.getProperty(IMMUTABLE_PROP) != null) {
            if (Boolean.parseBoolean(System.getProperty(IMMUTABLE_PROP))) {
                throw new IllegalStateException("Storage is immutable");
            }
        }
    }

    protected Path getSourcesDir() {
        return dir.resolve("sources");
    }

    protected Path getStoresDir() {
        return dir.resolve("stores");
    }

    protected Path getStreamsDir() {
        return dir.resolve("streams");
    }

    public synchronized Optional<DataSourceCollection> getCollectionForSourceEntry(DataSourceEntry entry) {
        if (entry == null) {
            return Optional.of(getInternalCollection());
        }

        return sourceCollections.stream()
                .filter(c -> c.getEntries().contains(entry))
                .findAny();
    }

    public synchronized Optional<DataSourceCollection> getCollectionForName(String name) {
        if (name == null) {
            return Optional.ofNullable(getInternalCollection());
        }

        return sourceCollections.stream()
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .findAny();
    }

    public synchronized List<DataStore> getUsableStores() {
        return new ArrayList<>(getStoreEntries().stream()
                .filter(entry -> !entry.isDisabled())
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

    public synchronized DataStoreEntry getStoreEntry(@NonNull DataStore store) {
        var entry = storeEntries.stream()
                .filter(n -> store.equals(n.getStore()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        return entry;
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        var entry =
                storeEntries.stream().filter(n -> store.equals(n.getStore())).findFirst();
        return entry;
    }

    public synchronized Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull String name) {
        return storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public synchronized Optional<DataSourceEntry> getDataSource(DataSourceReference ref) {
        Objects.requireNonNull(ref, "ref");

        switch (ref.getType()) {
            case LATEST -> {
                return Optional.ofNullable(latest);
            }
            case NAME -> {
                var found = sourceCollections.stream()
                        .map(col -> col.getEntries().stream()
                                .filter(e -> e.getName().equalsIgnoreCase(ref.getName()))
                                .findAny())
                        .flatMap(Optional::stream)
                        .toList();
                return Optional.ofNullable(found.size() == 1 ? found.get(0) : null);
            }
            case ID -> {
                var id = ref.getId();
                var col = id.getCollectionName() != null
                        ? sourceCollections.stream()
                                .filter(c ->
                                        c.getName() != null && c.getName().equalsIgnoreCase(id.getCollectionName()))
                                .findAny()
                        : Optional.of(getInternalCollection());
                if (col.isEmpty()) {
                    return Optional.empty();
                }

                return col.get().getEntries().stream()
                        .filter(e -> e.getName().equalsIgnoreCase(id.getEntryName()))
                        .findAny();
            }
        }

        throw new AssertionError();
    }

    public synchronized DataSourceId getId(DataSourceEntry entry) {
        Objects.requireNonNull(entry, "entry");
        if (!sourceEntries.contains(entry)) {
            throw new IllegalArgumentException("Entry not in storage");
        }

        var col = sourceCollections.stream()
                .filter(p -> p.getEntries().contains(entry))
                .findAny()
                .map(DataSourceCollection::getName)
                .orElse(null);
        var en = entry.getName();
        return DataSourceId.create(col, en);
    }

    public synchronized void add(DataSourceEntry e, DataSourceCollection c) {
        Objects.requireNonNull(e, "entry");

        if (c != null && !sourceCollections.contains(c)) {
            throw new IllegalArgumentException("Collection does not belong to the storage");
        }

        if (c == null) {
            c = getInternalCollection();
        }

        var id = DataSourceId.create(c.getName(), e.getName());
        if (getDataSource(DataSourceReference.id(id)).isPresent()) {
            throw new IllegalArgumentException("Entry with id " + id + " is already in storage");
        }

        checkImmutable();

        // Observe charset of all sources
        if (e.getSource() instanceof Charsettable cs) {
            AppCharsets.observe(cs.getCharset());
        }

        TrackEvent.withTrace("storage", "Adding new data source entry")
                .tag("name", e.getName())
                .tag("uuid", e.getUuid())
                .tag("collection", c)
                .handle();

        e.setDirectory(getSourcesDir().resolve("entries").resolve(e.getUuid().toString()));
        this.sourceEntries.add(e);
        c.addEntry(e);
        save();

        latest = e;
    }

    public void refreshAsync(StorageElement element, boolean deep) {
        ThreadHelper.runAsync(() -> {
            try {
                element.refresh(deep);
                propagateUpdate();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).reportable(false).handle();
            }
            save();
        });
    }

    private void propagateUpdate() {
        for (DataStoreEntry dataStoreEntry : getStoreEntries()) {
            dataStoreEntry.simpleRefresh();
        }

        for (var e : getSourceEntries()) {
            e.simpleRefresh();
        }
    }

    public void addStoreEntry(@NonNull DataStoreEntry e) {
        if (getStoreEntryIfPresent(e.getName()).isPresent()) {
            throw new IllegalArgumentException("Store with name " + e.getName() + " already exists");
        }

        synchronized (this) {
            e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
            this.storeEntries.add(e);
        }
        propagateUpdate();
        save();

        this.listeners.forEach(l -> l.onStoreAdd(e));
    }

    public void addStoreEntryIfNotPresent(@NonNull String name, DataStore store) {
        if (getStoreEntryIfPresent(store).isPresent()) {
            return;
        }

        var e = DataStoreEntry.createNew(UUID.randomUUID(), createUniqueStoreEntryName(name), store);
        addStoreEntry(e);
    }

    public DataStoreEntry addStoreEntry(@NonNull String name, DataStore store) {
        var e = DataStoreEntry.createNew(UUID.randomUUID(), createUniqueStoreEntryName(name), store);
        addStoreEntry(e);
        return e;
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        if (!store.getConfiguration().isDeletable()) {
            throw new UnsupportedOperationException();
        }

        synchronized (this) {
            this.storeEntries.remove(store);
        }
        propagateUpdate();
        save();
        this.listeners.forEach(l -> l.onStoreRemove(store));
    }

    public synchronized void addListener(StorageListener l) {
        this.listeners.add(l);
    }

    public DataSourceCollection createOrGetCollection(String name) {
        return getCollectionForName(name).orElseGet(() -> {
            var col = DataSourceCollection.createNew(name);
            addCollection(col);
            return col;
        });
    }

    public synchronized void addCollection(DataSourceCollection c) {
        checkImmutable();

        c.setDirectory(
                getSourcesDir().resolve("collections").resolve(c.getUuid().toString()));
        this.sourceCollections.add(c);
        this.listeners.forEach(l -> l.onCollectionAdd(c));
    }

    public synchronized void deleteCollection(DataSourceCollection c) {
        checkImmutable();

        this.sourceCollections.remove(c);
        this.listeners.forEach(l -> l.onCollectionRemove(c));

        c.getEntries().forEach(this::deleteSourceEntry);
    }

    public synchronized void deleteSourceEntry(DataSourceEntry e) {
        checkImmutable();

        this.sourceEntries.remove(e);

        var col = sourceCollections.stream()
                .filter(p -> p.getEntries().contains(e))
                .findAny();
        col.ifPresent(dataSourceCollection -> dataSourceCollection.removeEntry(e));
    }

    public abstract void load();

    public void refresh() {
        getStoreEntries().forEach(entry -> {
            entry.simpleRefresh();
        });
        save();
    }

    public abstract void save();

    public synchronized Optional<DataStoreEntry> getStoreEntry(UUID id) {
        return storeEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public synchronized Optional<DataSourceEntry> getSourceEntry(UUID id) {
        return sourceEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public synchronized Optional<DataSourceEntry> getSourceEntry(DataSource<?> source) {
        return sourceEntries.stream()
                .filter(e -> e.getSource() != null && e.getSource().equals(source))
                .findAny();
    }

    public synchronized Optional<DataSourceCollection> getCollection(UUID id) {
        return sourceCollections.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public synchronized List<DataSourceEntry> getSourceEntries() {
        return new ArrayList<>(sourceEntries);
    }

    public synchronized List<DataSourceCollection> getSourceCollections() {
        return new ArrayList<>(sourceCollections);
    }

    public synchronized List<DataStoreEntry> getStoreEntries() {
        return new ArrayList<>(storeEntries);
    }
}
