package io.xpipe.app.storage;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.secret.EncryptionKey;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.OsType;

import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.SecretKey;

public class StandardStorage extends DataStorage {

    private final Set<Path> directoriesToKeep = new HashSet<>();

    @Getter
    private final DataStorageSyncHandler dataStorageSyncHandler;

    @Getter
    private final DataStorageUserHandler dataStorageUserHandler;

    private final ReentrantLock busyIo = new ReentrantLock();
    private SecretKey vaultKey;

    @Getter
    private boolean disposed;

    private boolean saveQueued;

    StandardStorage() {
        this.dataStorageSyncHandler = DataStorageSyncHandler.getInstance();
        this.dataStorageUserHandler = DataStorageUserHandler.getInstance();
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

        try {
            FileUtils.forceMkdir(storesDir.toFile());
            FileUtils.forceMkdir(categoriesDir.toFile());
            FileUtils.forceMkdir(dataDir.toFile());
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

                        // We only keep invalid entries in developer mode as there's no point in keeping them in
                        // production.
                        if (AppProperties.get().isDevelopmentEnvironment()) {
                            directoriesToKeep.add(path);
                        }

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
            storeEntries.put(e, e);
            e.validate();
        }

        var local = DataStorage.get().getStoreEntry(LOCAL_ID);
        if (storeEntriesSet.stream().noneMatch(entry -> entry.getColor() != null)) {
            local.setColor(DataStoreColor.BLUE);
        }

        // Reload stores, this time with all entry refs present
        // These do however not have a completed validity yet
        refreshEntries();
        // Bring entries into completed validity if possible
        // Needed for chained stores
        refreshEntries();
        // Let providers work on complete stores
        callProviders();
        // Update validities after any possible changes
        refreshEntries();
        // Add any possible missing synthetic parents
        storeEntriesSet.forEach(entry -> {
            var syntheticParent = getSyntheticParent(entry);
            syntheticParent.ifPresent(entry1 -> {
                addStoreEntryIfNotPresent(entry1);
            });
        });
        // Update validities from synthetic parent changes
        refreshEntries();
        // Remove user inaccessible entries only when everything is valid, so we can check the parent hierarchies
        filterPerUserEntries(storeEntries.keySet());

        // Only add new stores if really necessary
        laterAddedEntries.stream()
                .filter(dataStoreEntry -> storeEntries.containsKey(dataStoreEntry))
                .forEach(e -> {
                    storeEntries.remove(e);
                    addStoreEntryIfNotPresent(e);
                });

        deleteLeftovers();

        this.dataStorageSyncHandler.afterStorageLoad();

        busyIo.unlock();
    }

    @Override
    public SecretKey getVaultKey() {
        return vaultKey;
    }

    public void load() {
        if (!busyIo.tryLock()) {
            return;
        }

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

        initVaultKey();

        AppMainWindow.loadingText("unlockingVault");

        try {
            dataStorageUserHandler.init();
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable("Unable to load vault users", e)
                    .terminal(true)
                    .build()
                    .handle();
        }
        dataStorageUserHandler.login();

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

        this.dataStorageSyncHandler.beforeStorageSave();

        try {
            FileUtils.forceMkdir(getStoresDir().toFile());
            FileUtils.forceMkdir(getCategoriesDir().toFile());
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
                var exists = Files.exists(e.getDirectory());
                var dirty = e.isDirty();
                e.writeDataToDisk();
                dataStorageSyncHandler.handleCategory(e, exists, dirty);
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
                        var exists = Files.exists(e.getDirectory());
                        var dirty = e.isDirty();
                        e.writeDataToDisk();
                        dataStorageSyncHandler.handleEntry(e, exists, dirty);
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

        deleteLeftovers();
        dataStorageUserHandler.save();
        dataStorageSyncHandler.afterStorageSave();
        if (dispose) {
            disposed = true;
        }

        busyIo.unlock();
        if (!dispose && saveQueued) {
            // Avoid stack overflow by doing it async
            saveAsync();
        }
    }

    @Override
    public boolean supportsSync() {
        return dataStorageSyncHandler.supportsSync();
    }

    private void filterPerUserEntries(Collection<DataStoreEntry> entries) {
        var toRemove = getStoreEntries().stream()
                .filter(dataStoreEntry -> shouldRemoveOtherUserEntry(dataStoreEntry))
                .toList();
        directoriesToKeep.addAll(toRemove.stream()
                .map(dataStoreEntry -> dataStoreEntry.getDirectory())
                .toList());
        toRemove.forEach(entries::remove);
    }

    private boolean shouldRemoveOtherUserEntry(DataStoreEntry entry) {
        var current = entry;
        while (true) {
            if (!current.getStoreNode().hasAccess()) {
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

    private void deleteLeftovers() {
        var storesDir = getStoresDir();
        var categoriesDir = getCategoriesDir();

        // Delete leftover directories in entries dir
        try (var s = Files.list(storesDir)) {
            s.forEach(file -> {
                if (directoriesToKeep.contains(file)) {
                    return;
                }

                var name = file.getFileName().toString();
                try {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(name);
                    } catch (Exception ex) {
                        FileUtils.forceDelete(file.toFile());
                        return;
                    }

                    var entry = getStoreEntryIfPresent(uuid);
                    if (entry.isEmpty()) {
                        TrackEvent.withTrace("Deleting leftover store directory")
                                .tag("uuid", uuid)
                                .handle();
                        FileUtils.forceDelete(file.toFile());
                        dataStorageSyncHandler.handleDeletion(file, uuid.toString());
                    }
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable(ex)
                            .expected()
                            .omit()
                            .build()
                            .handle();
                }
            });
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).terminal(true).build().handle();
        }

        // Delete leftover directories in categories dir
        try (var s = Files.list(categoriesDir)) {
            s.forEach(file -> {
                if (directoriesToKeep.contains(file)) {
                    return;
                }

                var name = file.getFileName().toString();
                try {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(name);
                    } catch (Exception ex) {
                        FileUtils.forceDelete(file.toFile());
                        return;
                    }

                    var entry = getStoreCategoryIfPresent(uuid);
                    if (entry.isEmpty()) {
                        TrackEvent.withTrace("Deleting leftover category directory")
                                .tag("uuid", uuid)
                                .handle();
                        FileUtils.forceDelete(file.toFile());
                        dataStorageSyncHandler.handleDeletion(file, uuid.toString());
                    }
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable(ex)
                            .expected()
                            .omit()
                            .build()
                            .handle();
                }
            });
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    private void initVaultKey() {
        var file = dir.resolve("vaultkey");
        try {
            if (Files.exists(file)) {
                var s = Files.readString(file);
                var id = new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
                vaultKey = EncryptionKey.getVaultSecretKey(id);
            } else {
                FileUtils.forceMkdir(dir.toFile());
                var id = UUID.randomUUID().toString();
                Files.writeString(file, Base64.getEncoder().encodeToString(id.getBytes(StandardCharsets.UTF_8)));
                vaultKey = EncryptionKey.getVaultSecretKey(id);
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(
                            "Unable to load vault key file " + file + " to decrypt vault contents. Is it corrupted?", e)
                    .terminal(true)
                    .build()
                    .handle();
        }
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
