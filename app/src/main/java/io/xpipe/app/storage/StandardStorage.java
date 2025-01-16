package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.EncryptionKey;
import io.xpipe.core.process.OsType;

import com.fasterxml.jackson.core.JacksonException;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.crypto.SecretKey;

public class StandardStorage extends DataStorage {

    private final List<Path> directoriesToKeep = new ArrayList<>();

    @Getter
    private final DataStorageSyncHandler dataStorageSyncHandler;

    @Getter
    private final DataStorageUserHandler dataStorageUserHandler;

    private SecretKey vaultKey;

    @Getter
    private boolean disposed;

    StandardStorage() {
        this.dataStorageSyncHandler = DataStorageSyncHandler.getInstance();
        this.dataStorageUserHandler = DataStorageUserHandler.getInstance();
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
            ErrorEvent.fromThrowable("Unable to create vault directory", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        try {
            initSystemInfo();
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Unable to load vault system info", e)
                    .build()
                    .handle();
        }

        try {
            initVaultKey();
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Unable to load vault key file", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        try {
            dataStorageUserHandler.init();
        } catch (IOException e) {
            ErrorEvent.fromThrowable("Unable to load vault users", e)
                    .terminal(true)
                    .build()
                    .handle();
        }
        dataStorageUserHandler.login();

        var storesDir = getStoresDir();
        var categoriesDir = getCategoriesDir();
        var dataDir = getDataDir();
        try {
            FileUtils.forceMkdir(storesDir.toFile());
            FileUtils.forceMkdir(categoriesDir.toFile());
            FileUtils.forceMkdir(dataDir.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable("Unable to create vault directory", e)
                    .terminal(true)
                    .build()
                    .handle();
        }

        try {
            var exception = new AtomicReference<Exception>();
            try (var cats = Files.list(categoriesDir)) {
                cats.filter(Files::isDirectory).forEach(path -> {
                    try {
                        try (Stream<Path> list = Files.list(path)) {
                            if (list.findAny().isEmpty()) {
                                return;
                            }
                        }

                        var c = DataStoreCategory.fromDirectory(path);
                        c.ifPresent(storeCategories::add);
                    } catch (IOException ex) {
                        // IO exceptions are not expected
                        exception.set(new IOException("Unable to load data from " + path + ". Is it corrupted?", ex));
                        directoriesToKeep.add(path);
                    } catch (Exception ex) {
                        // Data corruption and schema changes are expected
                        ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                    }
                });
            }

            // Show one exception
            if (exception.get() != null) {
                ErrorEvent.fromThrowable(exception.get()).handle();
            }

            setupBuiltinCategories();
            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();

            try (var dirs = Files.list(storesDir)) {
                dirs.filter(Files::isDirectory).forEach(path -> {
                    try {
                        try (Stream<Path> list = Files.list(path)) {
                            if (list.findAny().isEmpty()) {
                                return;
                            }
                        }

                        var entry = DataStoreEntry.fromDirectory(path);
                        if (entry.isEmpty()) {
                            return;
                        }

                        var foundCat = getStoreCategoryIfPresent(entry.get().getCategoryUuid());
                        if (foundCat.isEmpty()) {
                            entry.get().setCategoryUuid(null);
                        }

                        storeEntries.put(entry.get(), entry.get());
                    } catch (JacksonException ex) {
                        // Data corruption and schema changes are expected

                        // We only keep invalid entries in developer mode as there's no point in keeping them in
                        // production.
                        if (AppPrefs.get().isDevelopmentEnvironment()) {
                            directoriesToKeep.add(path);
                        }

                        ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                    } catch (IOException ex) {
                        // IO exceptions are not expected
                        exception.set(new IOException("Unable to load data from " + path + ". Is it corrupted?", ex));
                        directoriesToKeep.add(path);
                    } catch (Exception ex) {
                        // Data corruption and schema changes are expected

                        // We only keep invalid entries in developer mode as there's no point in keeping them in
                        // production.
                        if (AppPrefs.get().isDevelopmentEnvironment()) {
                            directoriesToKeep.add(path);
                        }

                        ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                    }
                });

                // Show one exception
                if (exception.get() != null) {
                    ErrorEvent.fromThrowable(exception.get()).expected().handle();
                }

                storeEntriesSet.forEach(e -> {
                    if (e.getCategoryUuid() == null
                            || getStoreCategoryIfPresent(e.getCategoryUuid()).isEmpty()) {
                        e.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }

                    if (e.getCategoryUuid() != null && e.getCategoryUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID)) {
                        e.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }

                    e.refreshIcon();
                });
            }
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
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
                    ErrorEvent.fromThrowable(ex)
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
            e.setConfiguration(
                    StorageElement.Configuration.builder().deletable(false).build());
            storeEntries.put(e, e);
            e.validate();
        }

        var local = DataStorage.get().getStoreEntry(LOCAL_ID);
        if (storeEntriesSet.stream().noneMatch(entry -> entry.getColor() != null)) {
            local.setColor(DataColor.BLUE);
        }

        // Reload stores, this time with all entry refs present
        // These do however not have a completed validity yet
        refreshEntries();
        // Bring entries into completed validity if possible
        // Needed for chained stores
        refreshEntries();
        // Let providers work on complete stores
        callProviders();
        // Update validaties after any possible changes
        refreshEntries();
        // Add any possible missing synthetic parents
        storeEntriesSet.forEach(entry -> {
            var syntheticParent = getSyntheticParent(entry);
            syntheticParent.ifPresent(entry1 -> {
                addStoreEntryIfNotPresent(entry1);
            });
        });
        // Update validaties from synthetic parent changes
        refreshEntries();
        // Remove user inaccessible entries only when everything is valid, so we can check the parent hierarchies
        filterPerUserEntries();

        if (!hasFixedLocal) {
            storeEntriesSet.removeIf(dataStoreEntry ->
                    !dataStoreEntry.getUuid().equals(LOCAL_ID) && dataStoreEntry.getStore() instanceof LocalStore);
            storeEntriesSet.stream()
                    .filter(entry -> entry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED)
                    .forEach(entry -> {
                        entry.dirty = true;
                        entry.setStoreNode(DataStorageNode.ofNewStore(entry.getStore()));
                    });
            // Save to apply changes
            save(false);
        }

        deleteLeftovers();

        loaded = true;
        busyIo.unlock();
        this.dataStorageSyncHandler.afterStorageLoad();
    }

    private void filterPerUserEntries() {
        var toRemove = getStoreEntries().stream()
                .filter(dataStoreEntry -> shouldRemoveOtherUserEntry(dataStoreEntry))
                .toList();
        directoriesToKeep.addAll(toRemove.stream()
                .map(dataStoreEntry -> dataStoreEntry.getDirectory())
                .toList());
        toRemove.forEach(storeEntries::remove);
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
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }

    public void save(boolean dispose) {
        try {
            // If another save operation is in progress, we have to wait on dispose
            // Otherwise the application may quit and kill the daemon thread that is performing the other save operation
            if (dispose && !busyIo.tryLock(1, TimeUnit.MINUTES)) {
                return;
            }
        } catch (InterruptedException e) {
            return;
        }

        // We don't need to wait on normal saves though
        if (!dispose && !busyIo.tryLock()) {
            return;
        }

        if (!loaded || disposed) {
            busyIo.unlock();
            return;
        }

        this.dataStorageSyncHandler.beforeStorageSave();

        try {
            FileUtils.forceMkdir(getStoresDir().toFile());
            FileUtils.forceMkdir(getCategoriesDir().toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e)
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
                ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
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
                        ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                    }
                });

        // Show one exception
        if (exception.get() != null) {
            ErrorEvent.fromThrowable(exception.get()).expected().handle();
        }

        deleteLeftovers();
        dataStorageUserHandler.save();
        dataStorageSyncHandler.afterStorageSave();
        if (dispose) {
            disposed = true;
        }
        busyIo.unlock();
    }

    @Override
    public boolean supportsSharing() {
        return dataStorageSyncHandler.supportsSync();
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
                    ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                }
            });
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
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
                    ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                }
            });
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    private void initVaultKey() throws IOException {
        var file = dir.resolve("vaultkey");
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
    }

    private void initSystemInfo() throws IOException {
        var file = dir.resolve("systeminfo");
        if (Files.exists(file)) {
            var read = Files.readString(file);
            if (!OsType.getLocal().getName().equals(read)) {
                ErrorEvent.fromMessage(
                                "This vault was originally created on a different system running " + read
                                        + ". Sharing connection information between systems directly might cause some problems."
                                        + " If you want to properly synchronize connection information across many systems, you can take a look into the git vault synchronization functionality in the settings.")
                        .expected()
                        .handle();
                var s = OsType.getLocal().getName();
                Files.writeString(file, s);
            }
        } else {
            FileUtils.forceMkdir(dir.toFile());
            var s = OsType.getLocal().getName();
            Files.writeString(file, s);
        }
    }
}
