package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStorageExtensionProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.LocalStore;

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

public class StandardStorage extends DataStorage {

    private final List<Path> directoriesToKeep = new ArrayList<>();

    @Getter
    private final GitStorageHandler gitStorageHandler;

    private String vaultKey;

    @Getter
    private boolean disposed;

    StandardStorage() {
        this.gitStorageHandler = GitStorageHandler.getInstance();
    }

    @Override
    public String getVaultKey() {
        return vaultKey;
    }

    public void load() {
        if (!busyIo.tryLock()) {
            return;
        }

        try {
            FileUtils.forceMkdir(dir.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).terminal(true).build().handle();
        }

        try {
            initSystemInfo();
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).build().handle();
        }

        try {
            initVaultKey();
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).terminal(true).build().handle();
        }

        var storesDir = getStoresDir();
        var categoriesDir = getCategoriesDir();
        var dataDir = getDataDir();
        try {
            FileUtils.forceMkdir(storesDir.toFile());
            FileUtils.forceMkdir(categoriesDir.toFile());
            FileUtils.forceMkdir(dataDir.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).terminal(true).build().handle();
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

                storeEntriesSet.forEach(dataStoreCategory -> {
                    if (dataStoreCategory.getCategoryUuid() == null
                            || getStoreCategoryIfPresent(dataStoreCategory.getCategoryUuid())
                                    .isEmpty()) {
                        dataStoreCategory.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }

                    if (dataStoreCategory.getCategoryUuid() != null &&
                            dataStoreCategory.getCategoryUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID)) {
                        dataStoreCategory.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }
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
            local.setColor(DataStoreColor.BLUE);
        }

        callProviders();
        refreshEntries();
        storeEntriesSet.forEach(entry -> {
            var syntheticParent = getSyntheticParent(entry);
            syntheticParent.ifPresent(entry1 -> {
                addStoreEntryIfNotPresent(entry1);
            });
        });
        refreshEntries();

        // Save to apply changes
        if (!hasFixedLocal) {
            storeEntriesSet.removeIf(dataStoreEntry ->
                    !dataStoreEntry.getUuid().equals(LOCAL_ID) && dataStoreEntry.getStore() instanceof LocalStore);
            storeEntriesSet.stream()
                    .filter(entry -> entry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED)
                    .forEach(entry -> {
                        entry.dirty = true;
                        entry.setStoreNode(DataStorageWriter.storeToNode(entry.getStore()));
                    });
            save(false);
        }

        deleteLeftovers();

        loaded = true;
        busyIo.unlock();
        this.gitStorageHandler.afterStorageLoad();
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

        this.gitStorageHandler.beforeStorageSave();

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
                gitStorageHandler.handleCategory(e, exists, dirty);
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
                        gitStorageHandler.handleEntry(e, exists, dirty);
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
        gitStorageHandler.afterStorageSave();
        if (dispose) {
            disposed = true;
        }
        busyIo.unlock();
    }

    @Override
    public boolean supportsSharing() {
        return gitStorageHandler.supportsShare();
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
                        gitStorageHandler.handleDeletion(file, uuid.toString());
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
                        gitStorageHandler.handleDeletion(file, uuid.toString());
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
            vaultKey = new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
        } else {
            Files.createDirectories(dir);
            vaultKey = UUID.randomUUID().toString();
            Files.writeString(file, Base64.getEncoder().encodeToString(vaultKey.getBytes(StandardCharsets.UTF_8)));
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
            Files.createDirectories(dir);
            var s = OsType.getLocal().getName();
            Files.writeString(file, s);
        }
    }
}
