package io.xpipe.app.storage;

import io.xpipe.app.core.AppCharsets;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.charsetter.Charsettable;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceId;
import io.xpipe.core.source.DataSourceReference;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.DataStoreProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
import io.xpipe.extension.util.ThreadHelper;
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

        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.storageInit();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
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
        if (INSTANCE == null) {
            throw new IllegalStateException("Not initialized");
        }

        return INSTANCE;
    }

    public DataSourceCollection getInternalCollection() {
        var found = sourceCollections.stream().filter(o -> o.getName().equals("Internal")).findAny();
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

    public String createUniqueSourceEntryName(DataSourceCollection col, DataSource<?> source) {
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
                    case TABLE -> I18n.get("table");
                    case STRUCTURE -> I18n.get("structure");
                    case TEXT -> I18n.get("text");
                    case RAW -> I18n.get("raw");
                    case COLLECTION -> I18n.get("collection");
                };
        return createUniqueSourceEntryName(col, typeName);
    }

    private String createUniqueSourceEntryName(DataSourceCollection col, String base) {
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

    public DataSourceEntry getLatest() {
        return latest;
    }

    public void setLatest(DataSourceEntry latest) {
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

    public Optional<DataSourceCollection> getCollectionForSourceEntry(DataSourceEntry entry) {
        if (entry == null) {
            return Optional.of(getInternalCollection());
        }

        return sourceCollections.stream()
                .filter(c -> c.getEntries().contains(entry))
                .findAny();
    }

    public Optional<DataSourceCollection> getCollectionForName(String name) {
        return sourceCollections.stream()
                .filter(c -> name.equalsIgnoreCase(c.getName()))
                .findAny();
    }

    public Optional<DataStoreEntry> getStoreIfPresent(@NonNull String name) {
        return storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public void renameStore(DataStoreEntry entry, String name) {
        if (getStoreIfPresent(name).isPresent()) {
            throw new IllegalArgumentException("Store with name " + name + " already exists");
        }

        entry.setName(name);
    }

    public DataStoreEntry getStore(@NonNull String name, boolean acceptDisabled) {
        var entry = storeEntries.stream()
                .filter(n -> n.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store with name " + name + " not found"));
        if (!acceptDisabled && entry.isDisabled()) {
            throw new IllegalArgumentException("Store with name " + name + " is disabled");
        }
        return entry;
    }

    public DataStoreEntry getStore(@NonNull DataStore store) {
        var entry = storeEntries.stream()
                .filter(n -> store.equals(n.getStore()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        return entry;
    }

    public Optional<DataStoreEntry> getStoreEntryIfPresent(@NonNull DataStore store) {
        var entry =
                storeEntries.stream().filter(n -> store.equals(n.getStore())).findFirst();
        return entry;
    }

    public Optional<DataSourceEntry> getDataSource(DataSourceReference ref) {
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

    public DataSourceId getId(DataSourceEntry entry) {
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

    public void add(DataSourceEntry e, DataSourceCollection c) {
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

    private void propagateUpdate() throws Exception {
        for (DataStoreEntry dataStoreEntry : getStores()) {
            dataStoreEntry.refresh(false);
        }

        for (var dataStoreEntry : sourceEntries) {
            dataStoreEntry.refresh(false);
        }
    }

    public void addStore(@NonNull DataStoreEntry e) {
        if (getStoreIfPresent(e.getName()).isPresent()) {
            throw new IllegalArgumentException("Store with name " + e.getName() + " already exists");
        }

        e.setDirectory(getStoresDir().resolve(e.getUuid().toString()));
        this.storeEntries.add(e);
        save();

        this.listeners.forEach(l -> l.onStoreAdd(e));
    }

    public DataStoreEntry addStore(@NonNull String name, DataStore store) {
        var e = DataStoreEntry.createNew(UUID.randomUUID(), name, store);
        addStore(e);
        return e;
    }

    public void deleteStoreEntry(@NonNull DataStoreEntry store) {
        if (!store.getConfiguration().isDeletable()) {
            throw new UnsupportedOperationException();
        }

        this.storeEntries.remove(store);
        save();
        this.listeners.forEach(l -> l.onStoreRemove(store));
    }

    public void addListener(StorageListener l) {
        this.listeners.add(l);
    }

    public DataSourceCollection createOrGetCollection(String name) {
        return getCollectionForName(name).orElseGet(() -> {
            var col = DataSourceCollection.createNew(name);
            addCollection(col);
            return col;
        });
    }

    public void addCollection(DataSourceCollection c) {
        checkImmutable();

        c.setDirectory(
                getSourcesDir().resolve("collections").resolve(c.getUuid().toString()));
        this.sourceCollections.add(c);
        this.listeners.forEach(l -> l.onCollectionAdd(c));
    }

    public void deleteCollection(DataSourceCollection c) {
        checkImmutable();

        this.sourceCollections.remove(c);
        this.listeners.forEach(l -> l.onCollectionRemove(c));

        c.getEntries().forEach(this::deleteEntry);
    }

    public void deleteEntry(DataSourceEntry e) {
        checkImmutable();

        this.sourceEntries.remove(e);

        var col = sourceCollections.stream()
                .filter(p -> p.getEntries().contains(e))
                .findAny();
        col.ifPresent(dataSourceCollection -> dataSourceCollection.removeEntry(e));
    }

    public abstract void load();

    public void refresh() {
        storeEntries.forEach(entry -> {
            try {
                entry.refresh(false);
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().reportable(false).handle();
            }
        });
        save();
    }

    public abstract void save();

    public Optional<DataStoreEntry> getStoreEntryByUuid(UUID id) {
        return storeEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public Optional<DataSourceEntry> getSourceEntryByUuid(UUID id) {
        return sourceEntries.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public Optional<DataSourceEntry> getDataSourceEntryById(DataSourceId id) {
        return sourceEntries.stream().filter(e -> getId(e).equals(id)).findAny();
    }

    public Optional<DataSourceEntry> getEntryBySource(DataSource<?> source) {
        return sourceEntries.stream()
                .filter(e -> e.getSource() != null && e.getSource().equals(source))
                .findAny();
    }

    public Optional<DataStoreEntry> getEntryByStore(DataStore store) {
        return storeEntries.stream()
                .filter(e -> e.getStore() != null && e.getStore().equals(store))
                .findAny();
    }

    public Optional<DataSourceCollection> getCollectionByUuid(UUID id) {
        return sourceCollections.stream().filter(e -> e.getUuid().equals(id)).findAny();
    }

    public List<DataSourceEntry> getSourceEntries() {
        return Collections.unmodifiableList(sourceEntries);
    }

    public List<DataSourceCollection> getSourceCollections() {
        return Collections.unmodifiableList(sourceCollections);
    }

    public List<DataStoreEntry> getStores() {
        return Collections.unmodifiableList(storeEntries);
    }
}
