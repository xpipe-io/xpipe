package io.xpipe.app.storage;

import io.xpipe.app.comp.storage.store.StoreSortMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.FeatureProvider;
import io.xpipe.app.util.XPipeSession;
import io.xpipe.core.store.LocalStore;
import lombok.Getter;
import lombok.NonNull;
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

    StandardStorage() {
        this.gitStorageHandler = FeatureProvider.get().createStorageHandler();
        this.gitStorageHandler.init(dir);
    }

    private boolean isNewSession() {
        return XPipeSession.get().isNewSystemSession();
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

    public synchronized void load() {
        this.gitStorageHandler.prepareForLoad();

        var newSession = isNewSession();
        var storesDir = getStoresDir();
        var categoriesDir = getCategoriesDir();

        try {
            FileUtils.forceMkdir(storesDir.toFile());
            FileUtils.forceMkdir(categoriesDir.toFile());
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
                        storeCategories.add(c);
                    } catch (IOException ex) {
                        // IO exceptions are not expected
                        exception.set(ex);
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

            if (getStoreCategoryIfPresent(ALL_CATEGORY_UUID).isEmpty()) {
                var cat = DataStoreCategory.createNew(null, ALL_CATEGORY_UUID,"All connections");
                cat.setDirectory(categoriesDir.resolve(ALL_CATEGORY_UUID.toString()));
                storeCategories.add(cat);
            }

            if (getStoreCategoryIfPresent(SCRIPTS_CATEGORY_UUID).isEmpty()) {
                var cat = DataStoreCategory.createNew(null, SCRIPTS_CATEGORY_UUID,"All scripts");
                cat.setDirectory(categoriesDir.resolve(SCRIPTS_CATEGORY_UUID.toString()));
                storeCategories.add(cat);
            }

            if (getStoreCategoryIfPresent(PREDEFINED_SCRIPTS_CATEGORY_UUID).isEmpty()) {
                var cat = DataStoreCategory.createNew(SCRIPTS_CATEGORY_UUID, PREDEFINED_SCRIPTS_CATEGORY_UUID,"Predefined");
                cat.setDirectory(categoriesDir.resolve(PREDEFINED_SCRIPTS_CATEGORY_UUID.toString()));
                storeCategories.add(cat);
            }

            if (getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).isEmpty()) {
                storeCategories.add(new DataStoreCategory(
                        categoriesDir.resolve(DEFAULT_CATEGORY_UUID.toString()),
                        DEFAULT_CATEGORY_UUID,
                        "Default",
                        Instant.now(),
                        Instant.now(),
                        true,
                        ALL_CATEGORY_UUID,
                        StoreSortMode.ALPHABETICAL_ASC, false
                ));
            }

            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();

            storeCategories.forEach(dataStoreCategory -> {
                if (dataStoreCategory.getParentCategory() != null
                        && getStoreCategoryIfPresent(dataStoreCategory.getParentCategory())
                                .isEmpty()) {
                    dataStoreCategory.setParentCategory(ALL_CATEGORY_UUID);
                } else if (dataStoreCategory.getParentCategory() == null && !dataStoreCategory.getUuid().equals(ALL_CATEGORY_UUID) && !dataStoreCategory.getUuid().equals(SCRIPTS_CATEGORY_UUID)) {
                    dataStoreCategory.setParentCategory(ALL_CATEGORY_UUID);
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
                        if (entry == null) {
                            return;
                        }

                        var foundCat = getStoreCategoryIfPresent(entry.getCategoryUuid());
                        if (foundCat.isEmpty()) {
                            entry.setCategoryUuid(null);
                        }

                        storeEntries.add(entry);
                    } catch (IOException ex) {
                        // IO exceptions are not expected
                        exception.set(ex);
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

                storeEntries.forEach(dataStoreCategory -> {
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

        {
            var hasFixedLocal = storeEntries.stream().anyMatch(dataStoreEntry -> dataStoreEntry.getUuid().equals(LOCAL_ID));
            // storeEntries.removeIf(dataStoreEntry -> !dataStoreEntry.getUuid().equals(LOCAL_ID) && dataStoreEntry.getStore() instanceof LocalStore);
            if (!hasFixedLocal) {
                var e = DataStoreEntry.createNew(
                        LOCAL_ID, DataStorage.DEFAULT_CATEGORY_UUID, "Local Machine", new LocalStore());
                e.setConfiguration(
                        StorageElement.Configuration.builder().deletable(false).build());
                storeEntries.add(e);
                e.validate();
            }
        }

        // Refresh to update state
        storeEntries.forEach(dataStoreEntry -> dataStoreEntry.refresh());

        refreshValidities(true);

        deleteLeftovers();
    }

    public synchronized void save() {
        this.gitStorageHandler.prepareForSave();

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

        storeEntries.stream()
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
        gitStorageHandler.postSave();
    }

    @Override
    public Path getInternalStreamPath(@NonNull UUID uuid) {
        return getStreamsDir().resolve(uuid.toString());
    }

    @Override
    public boolean supportsSharing() {
        return gitStorageHandler.supportsShare();
    }
}
