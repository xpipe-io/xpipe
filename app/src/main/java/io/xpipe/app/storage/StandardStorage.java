package io.xpipe.app.storage;

import io.xpipe.app.core.AppLayoutModel;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.store.LocalStore;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.OsType;
import io.xpipe.app.util.ThreadHelper;

import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class StandardStorage extends DataStorage {

    @Getter
    private final DataStorageSyncHandler dataStorageSyncHandler;

    @Getter
    private DataStorageAccessHandler dataStorageAccessHandler;

    private final ReentrantLock busyIo = new ReentrantLock();

    @Getter
    private boolean disposed;

    private boolean saveQueued;

    private final AppLayoutModel.QueueEntry queueEntry =
            AppLayoutModel.QueueEntry.ofNotification("syncInProgressTitle", "syncInProgress", "mdi2g-git", false);

    StandardStorage() {
        this.dataStorageSyncHandler = DataStorageSyncHandler.getInstance();
    }

    public void pullManually() {
        if (!busyIo.tryLock()) {
            return;
        }
        dataStorageSyncHandler.pullManually();
        busyIo.unlock();
    }

    @Override
    public void pushManually() {
        if (!busyIo.tryLock()) {
            return;
        }
        dataStorageSyncHandler.pushManually();
        busyIo.unlock();
    }

    private void startSyncWatcher() {
        GlobalTimer.scheduleUntil(Duration.ofSeconds(20), false, () -> {
            ThreadHelper.runAsync(() -> {
                if (!busyIo.tryLock()) {
                    return;
                }
                dataStorageSyncHandler.refreshRemoteData();
                busyIo.unlock();
            });
            return false;
        });
    }

    public void reloadContent() {
        if (AppOperationMode.isInShutdown()) {
            return;
        }

        busyIo.lock();

        var initialLoad = getStoreEntries().size() == 0;
        var storesDir = getStoresDir();
        var categoriesDir = getCategoriesDir();
        var dataDir = getDataDir();
        var iconsDir = getIconsDir();

        try {
            FileUtils.forceMkdir(storesDir.toFile());
            FileUtils.forceMkdir(categoriesDir.toFile());
            FileUtils.forceMkdir(dataDir.toFile());
            FileUtils.forceMkdir(iconsDir.toFile());
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable("Unable to create vault directory", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        for (DataStoreCategory cat : new ArrayList<>(storeCategories)) {
            if (Arrays.stream(cat.getShareableFiles()).noneMatch(Files::exists)) {
                deleteStoreCategory(cat, false, false);
            }
        }

        var laterAddedEntries = new HashSet<DataStoreEntry>();
        try {
            var exception = new AtomicReference<Exception>();
            try (var cats = Files.list(categoriesDir)) {
                cats.filter(Files::isDirectory).forEach(path -> {
                    try {
                        var c = DataStoreCategory.fromDirectory(path);
                        if (c.isEmpty()) {
                            return;
                        }

                        if (initialLoad) {
                            storeCategories.add(c.get());
                            return;
                        }

                        var existing = getStoreCategoryIfPresent(c.get().getUuid());
                        if (existing.isPresent()) {
                            if (existing.get().isChangedForReload(c.get())) {
                                updateCategory(existing.get(), c.get());
                            }
                            return;
                        }

                        addStoreCategory(c.get());
                    } // IO exceptions are not expected
                    catch (Exception ex) {
                        // Data corruption and schema changes are expected
                        ErrorEventFactory.fromThrowable(ex)
                                .expected()
                                .omit()
                                .build()
                                .handle();
                    }
                });
            }

            // Show one exception
            if (exception.get() != null) {
                ErrorEventFactory.fromThrowable(exception.get()).handle();
            }

            setupBuiltinCategories();
            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();

            for (DataStoreEntry entry : new ArrayList<>(getStoreEntries())) {
                if (Arrays.stream(entry.getShareableFiles()).noneMatch(Files::exists)) {
                    deleteStoreEntry(entry);
                }
            }

            try (var dirs = Files.list(storesDir)) {
                dirs.filter(Files::isDirectory).forEach(path -> {
                    try {
                        var entry = DataStoreEntry.fromDirectory(path);
                        if (entry.isEmpty()) {
                            return;
                        }

                        if (initialLoad) {
                            var foundCat = getStoreCategoryIfPresent(entry.get().getCategoryUuid());
                            if (foundCat.isEmpty()) {
                                entry.get().setCategoryUuid(null);
                            }

                            storeEntries.put(entry.get(), entry.get());
                            return;
                        }

                        var existing = getStoreEntryIfPresent(entry.get().getUuid());
                        if (existing.isPresent()) {
                            if (existing.get().isChangedForReload(entry.get())) {
                                updateEntry(existing.get(), entry.get());
                            }
                            return;
                        }

                        laterAddedEntries.add(entry.get());
                        storeEntries.put(entry.get(), entry.get());
                    } // IO exceptions are not expected
                    catch (Exception ex) {
                        // Data corruption and schema changes are expected

                        ErrorEventFactory.fromThrowable(ex)
                                .expected()
                                .omit()
                                .build()
                                .handle();
                    }
                });

                // Show one exception
                if (exception.get() != null) {
                    ErrorEventFactory.fromThrowable(exception.get()).expected().handle();
                }

                storeEntriesSet.forEach(e -> {
                    if (e.getCategoryUuid() == null
                            || getStoreCategoryIfPresent(e.getCategoryUuid()).isEmpty()) {
                        e.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }

                    if (e.getCategoryUuid() != null && e.getCategoryUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID)) {
                        e.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }
                });
            }
        } catch (IOException ex) {
            ErrorEventFactory.fromThrowable(ex).terminal(true).build().handle();
        }

        var hasFixedLocal = storeEntriesSet.stream()
                .anyMatch(dataStoreEntry -> dataStoreEntry.getUuid().equals(LOCAL_ID));
        if (hasFixedLocal) {
            var local = getStoreEntry(LOCAL_ID);
            if (local.getValidity() == DataStoreEntry.Validity.LOAD_FAILED) {
                try {
                    storeEntries.remove(local);
                    local.deleteFromDisk();
                    hasFixedLocal = false;
                } catch (IOException ex) {
                    ErrorEventFactory.fromThrowable(ex)
                            .terminal(true)
                            .expected()
                            .build()
                            .handle();
                }
            }
        }

        if (!hasFixedLocal) {
            var e = DataStoreEntry.createNew(
                    LOCAL_ID, DataStorage.DEFAULT_CATEGORY_UUID, "Local Machine", new LocalStore());
            e.setDirectory(getStoresDir().resolve(LOCAL_ID.toString()));
            e.setOrderIndex(getNextOrderIndex());
            storeEntries.put(e, e);
            e.validate();
        }

        var local = DataStorage.get().getStoreEntry(LOCAL_ID);
        if (storeEntriesSet.stream().noneMatch(entry -> entry.getColor() != null)) {
            local.setColor(DataStoreColor.BLUE);
        }

        // Reload stores, this time with all entry refs present
        // These do however not have a completed validity yet
        refreshStoreEntries();
        // Bring entries into completed validity if possible
        // Needed for chained stores
        refreshStoreEntries();
        if (initialLoad) {
            // Let providers work on complete stores
            callProviders();
        }
        // Update validities after any possible changes
        refreshStoreEntries();
        // Add any possible missing synthetic parents
        storeEntriesSet.forEach(entry -> {
            var syntheticParent = getSyntheticParent(entry);
            syntheticParent.ifPresent(entry1 -> {
                addStoreEntryIfNotPresent(entry1);
            });
        });
        entriesAvailable = true;
        // Update validities from synthetic parent changes and entries available flag changes
        refreshStoreEntries();

        // The principals might have changed externally
        refreshStoreEntriesEncryption();

        double maxOrderIndex = 0;
        for (DataStoreEntry e : storeEntriesSet) {
            if (e.getOrderIndex() > maxOrderIndex) {
                maxOrderIndex = e.orderIndex;
            }
        }
        for (DataStoreCategory c : storeCategories) {
            if (c.getOrderIndex() > maxOrderIndex) {
                maxOrderIndex = c.getOrderIndex();
            }
        }
        orderCounter = (int) Math.ceil(maxOrderIndex + 1.0);

        // Remove inaccessible entries only when everything is valid, so we can check the parent hierarchies
        filterInaccessibleEntries(storeEntries.keySet());

        // Only add new stores if really necessary
        laterAddedEntries.stream()
                .filter(dataStoreEntry -> storeEntries.containsKey(dataStoreEntry))
                .forEach(e -> {
                    storeEntries.remove(e);
                    addStoreEntryIfNotPresent(e);
                });

        // Refresh validities after entries have potentially been removed
        refreshStoreEntries();

        this.dataStorageSyncHandler.afterStorageLoad();

        busyIo.unlock();
    }

    public void load() {
        if (!busyIo.tryLock()) {
            return;
        }

        var dirExists = Files.isDirectory(dir);

        try {
            FileUtils.forceMkdir(dir.toFile());
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable("Unable to create vault directory", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        try {
            initSystemInfo();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable("Unable to load vault system info", e)
                    .build()
                    .handle();
        }

        try {
            if (!dirExists) {
                Files.writeString(
                        dir.resolve("vaultversion"), AppProperties.get().getVersion());
            }

            DataStorageCompatibilityCheck.showLegacyVaultMigrationErrorIfNeeded();

            Files.writeString(dir.resolve("vaultversion"), AppProperties.get().getVersion());
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable("Unable to load vault version data", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        try {
            dataStorageAccessHandler = DataStorageAccessHandler.getInstance();
            dataStorageAccessHandler.init();
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable("Unable to load vault access data", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        if (dataStorageAccessHandler.isAccessRestricted()) {
            AppMainWindow.loadingText("unlockingVault");
        }

        dataStorageAccessHandler.login();

        reloadContent();

        busyIo.unlock();

        startSyncWatcher();

        // Full save on initial load
        saveAsync();
    }

    public void saveAsync() {
        // If we are already loading or saving, don't queue up another operation.
        // This could otherwise lead to thread starvation with virtual threads

        // Technically the load and save operations also return instantly if locked, but let's not even create new
        // threads here

        // Technically we would have to synchronize the saveQueued update to avoid a rare lost update
        // but in practice it doesn't really matter as the save queueing is optional
        // The last dispose save will save everything anyway, it's about optimizing before that
        if (busyIo.isLocked()) {
            saveQueued = true;
            return;
        }

        ThreadHelper.runAsync(() -> {
            save(false);
        });
    }

    public void save(boolean dispose) {
        try {
            // If another save operation is in progress, we have to wait on dispose
            // Otherwise the application may quit and kill the daemon thread that is performing the other save operation
            if (dispose && !busyIo.tryLock(1, TimeUnit.MINUTES)) {
                disposed = true;
                return;
            }
        } catch (InterruptedException e) {
            return;
        }

        // We don't need to wait on normal saves though
        if (!dispose && !busyIo.tryLock()) {
            saveQueued = true;
            return;
        }

        if (disposed) {
            busyIo.unlock();
            return;
        }

        this.saveQueued = false;

        var saveActive = new AtomicBoolean(true);
        var syncEnabled = dataStorageSyncHandler.supportsSync();
        if (syncEnabled) {
            GlobalTimer.delay(
                    () -> {
                        if (saveActive.get()) {
                            AppLayoutModel.get().showQueueEntry(queueEntry, null, false);
                        }
                    },
                    Duration.ofSeconds(5));
        }

        this.dataStorageSyncHandler.beforeStorageSave();

        try {
            FileUtils.forceMkdir(getStoresDir().toFile());
            FileUtils.forceMkdir(getCategoriesDir().toFile());
            FileUtils.forceMkdir(getDataDir().toFile());
            FileUtils.forceMkdir(getIconsDir().toFile());
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e)
                    .description("Unable to create storage directory " + getStoresDir())
                    .terminal(true)
                    .build()
                    .handle();
        }

        var exception = new AtomicReference<Exception>();

        storeCategories.forEach(e -> {
            try {
                synchronized (dir) {
                    var exists = Files.exists(e.getDirectory());
                    var dirty = e.isDirty();
                    e.writeDataToDisk();
                    dataStorageSyncHandler.handleCategory(e, exists, dirty);
                }
            } catch (IOException ex) {
                // IO exceptions are not expected
                exception.set(ex);
            } catch (Exception ex) {
                // Data corruption and schema changes are expected
                ErrorEventFactory.fromThrowable(ex).expected().omit().build().handle();
            }
        });

        storeEntriesSet.stream()
                .filter(dataStoreEntry -> dataStoreEntry.shouldSave())
                .forEach(e -> {
                    try {
                        synchronized (dir) {
                            var exists = Files.exists(e.getDirectory());
                            var dirty = e.isDirty();
                            e.writeDataToDisk();
                            dataStorageSyncHandler.handleEntry(e, exists, dirty);
                        }
                    } catch (Exception ex) {
                        // Data corruption and schema changes are expected
                        exception.set(ex);
                        ErrorEventFactory.fromThrowable(ex)
                                .expected()
                                .omit()
                                .build()
                                .handle();
                    }
                });

        // Show one exception
        if (exception.get() != null) {
            ErrorEventFactory.fromThrowable(exception.get()).expected().handle();
        }

        dataStorageAccessHandler.save();
        dataStorageSyncHandler.afterStorageSave(true, dispose);
        if (dispose) {
            disposed = true;
        }

        saveActive.set(false);
        queueEntry.hide();

        busyIo.unlock();
        if (!dispose && saveQueued) {
            // Avoid stack overflow by doing it async
            saveAsync();
        }
    }

    @Override
    protected void deleteStoreEntryFromDisk(DataStoreEntry entry) {
        var dir = entry.getDirectory();
        if (dir != null) {
            try {
                synchronized (this.dir) {
                    FileUtils.deleteDirectory(dir.toFile());
                    dataStorageSyncHandler.handleDeletion(dir, entry.getName());
                }
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        }
    }

    @Override
    protected void deleteStoreCategoryFromDisk(DataStoreCategory cat) {
        var dir = cat.getDirectory();
        if (dir != null) {
            try {
                FileUtils.deleteDirectory(dir.toFile());
                dataStorageSyncHandler.handleDeletion(dir, cat.getName());
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).expected().handle();
            }
        }
    }

    @Override
    public Optional<DataStoreEntry> getInaccessibleEntry(UUID uuid) {
        return storeEntriesInaccessible.keySet().stream()
                .filter(entry -> entry.getUuid().equals(uuid))
                .findFirst();
    }

    @Override
    public boolean supportsSync() {
        return dataStorageSyncHandler.supportsSync();
    }

    private void filterInaccessibleEntries(Collection<DataStoreEntry> entries) {
        var toRemove = getStoreEntries().stream()
                .filter(dataStoreEntry -> shouldRemoveInaccessibleEntry(dataStoreEntry))
                .toList();
        toRemove.forEach(entries::remove);
        toRemove.forEach(entry -> storeEntriesInaccessible.put(entry, entry));
    }

    private boolean shouldRemoveInaccessibleEntry(DataStoreEntry entry) {
        var current = entry;
        while (true) {
            // Encrypted for someone else
            if (!current.isAccessible()) {
                return true;
            }

            // We can read the data as it is not encrypted
            // but the scope is still not available to us
            if (!current.getAccessScope().isAccessible()) {
                return true;
            }

            var parent = getDefaultDisplayParent(current);
            if (parent.isEmpty()) {
                return false;
            } else {
                current = parent.get();
            }
        }
    }

    private void callProviders() {
        DataStorageExtensionProvider.getAll().forEach(p -> {
            try {
                p.storageInit();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
            }
        });
    }

    private void initSystemInfo() throws IOException {
        var file = dir.resolve("systeminfo");
        if (Files.exists(file)) {
            var read = Files.readString(file);
            if (!OsType.ofLocal().getName().equals(read)) {
                ErrorEventFactory.fromMessage(
                                "This vault was originally created on a different system running " + read
                                        + ". Sharing the same data directory between systems directly will cause some problems."
                                        + " If you want to properly synchronize connection information across many systems, you can take a look into the git vault synchronization functionality in the settings. It also supports local directory git repositories.")
                        .documentationLink(DocumentationLink.SYNC_LOCAL)
                        .expected()
                        .handle();
                var s = OsType.ofLocal().getName();
                Files.writeString(file, s);
            }
        } else {
            FileUtils.forceMkdir(dir.toFile());
            var s = OsType.ofLocal().getName();
            Files.writeString(file, s);
        }
    }
}
