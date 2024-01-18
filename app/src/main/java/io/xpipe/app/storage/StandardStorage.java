package io.xpipe.app.storage;

import io.xpipe.app.comp.store.StoreSortMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.store.LocalStore;
import lombok.Getter;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class StandardStorage extends DataStorage {

    private final List<Path> directoriesToKeep = new ArrayList<>();
    @Getter
    private final GitStorageHandler gitStorageHandler;

    @Getter
    private boolean disposed;

    StandardStorage() {
        this.gitStorageHandler = GitStorageHandler.getInstance();
        this.gitStorageHandler.init(dir);
    }

    @Override
    protected void onReset() {
        gitStorageHandler.onReset();
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
                        TrackEvent.withTrace("storage", "Deleting leftover store directory")
                                .tag("uuid", uuid)
                                .handle();
                        FileUtils.forceDelete(file.toFile());
                        gitStorageHandler.handleDeletion(file,uuid.toString());
                    }
                } catch (Exception ex) {
                    ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
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
                        TrackEvent.withTrace("storage", "Deleting leftover category directory")
                                .tag("uuid", uuid)
                                .handle();
                        FileUtils.forceDelete(file.toFile());
                        gitStorageHandler.handleDeletion(file,uuid.toString());
                    }
                } catch (Exception ex) {
                    ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                }
            });
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    public void load() {
        if (!busyIo.tryLock()) {
            return;
        }

        this.gitStorageHandler.beforeStorageLoad();

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
                        exception.set(new IOException("Unable to load data from " + path.toString() + ". Is it corrupted?", ex));
                        directoriesToKeep.add(path);
                    }  catch (Exception ex) {
                        // Data corruption and schema changes are expected
                        ErrorEvent.fromThrowable(ex).expected().omit().build().handle();
                    }
                });
            }

            // Show one exception
            if (exception.get() != null) {
                ErrorEvent.fromThrowable(exception.get()).handle();
            }

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
                cat.setShare(true);
                storeCategories.add(cat);
            }

            if (getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).isEmpty()) {
                var cat = new DataStoreCategory(categoriesDir.resolve(DEFAULT_CATEGORY_UUID.toString()), DEFAULT_CATEGORY_UUID, "Default",
                        Instant.now(), Instant.now(), true, ALL_CONNECTIONS_CATEGORY_UUID, StoreSortMode.ALPHABETICAL_ASC, true);
                storeCategories.add(cat);
            }

            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();

            storeCategories.forEach(dataStoreCategory -> {
                if (dataStoreCategory.getParentCategory() != null
                        && getStoreCategoryIfPresent(dataStoreCategory.getParentCategory())
                                .isEmpty()) {
                    dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
                } else if (dataStoreCategory.getParentCategory() == null && !dataStoreCategory.getUuid().equals(ALL_CONNECTIONS_CATEGORY_UUID) && !dataStoreCategory.getUuid().equals(
                        ALL_SCRIPTS_CATEGORY_UUID)) {
                    dataStoreCategory.setParentCategory(ALL_CONNECTIONS_CATEGORY_UUID);
                }
            });

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
                        exception.set(new IOException("Unable to load data from " + path.toString() + ". Is it corrupted?", ex));
                        directoriesToKeep.add(path);
                    }  catch (Exception ex) {
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
                    ErrorEvent.fromThrowable(exception.get()).handle();
                }

                storeEntriesSet.forEach(dataStoreCategory -> {
                    if (dataStoreCategory.getCategoryUuid() == null
                            || getStoreCategoryIfPresent(dataStoreCategory.getCategoryUuid())
                            .isEmpty()) {
                        dataStoreCategory.setCategoryUuid(DEFAULT_CATEGORY_UUID);
                    }
                });
            }
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }

            var hasFixedLocal = storeEntriesSet.stream().anyMatch(dataStoreEntry -> dataStoreEntry.getUuid().equals(LOCAL_ID));
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

        refreshValidities(true);
        storeEntriesSet.forEach(entry -> {
            var syntheticParent = getSyntheticParent(entry);
            syntheticParent.ifPresent(entry1 -> {
                addStoreEntryIfNotPresent(entry1);
            });
        });
        refreshValidities(true);

        // Save to apply changes
        if (!hasFixedLocal) {
            storeEntriesSet.removeIf(dataStoreEntry -> !dataStoreEntry.getUuid().equals(LOCAL_ID) && dataStoreEntry.getStore() instanceof LocalStore);
            storeEntriesSet.stream().filter(entry -> entry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED).forEach(entry -> {
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

    public void save(boolean dispose) {
        if (!loaded || disposed) {
            return;
        }

        if (!busyIo.tryLock()) {
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
            }  catch (Exception ex) {
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
            ErrorEvent.fromThrowable(exception.get()).handle();
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
}
