package io.xpipe.app.storage;

import io.xpipe.app.comp.store.StoreSortMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.store.LocalStore;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;

public class ImpersistentStorage extends DataStorage {

    @Override
    public String getVaultKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void load() {
        var storesDir = getStoresDir();
        var categoriesDir = getCategoriesDir();

        {
            var cat = DataStoreCategory.createNew(null, ALL_CONNECTIONS_CATEGORY_UUID, "All connections");
            cat.setDirectory(categoriesDir.resolve(ALL_CONNECTIONS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }
        {
            var cat = DataStoreCategory.createNew(null, ALL_SCRIPTS_CATEGORY_UUID, "All scripts");
            cat.setDirectory(categoriesDir.resolve(ALL_SCRIPTS_CATEGORY_UUID.toString()));
            storeCategories.add(cat);
        }
        {
            var cat = new DataStoreCategory(
                    categoriesDir.resolve(DEFAULT_CATEGORY_UUID.toString()),
                    DEFAULT_CATEGORY_UUID,
                    "Default",
                    Instant.now(),
                    Instant.now(),
                    true,
                    ALL_CONNECTIONS_CATEGORY_UUID,
                    StoreSortMode.ALPHABETICAL_ASC,
                    true);
            storeCategories.add(cat);
            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
        }

        var e = DataStoreEntry.createNew(
                LOCAL_ID, DataStorage.DEFAULT_CATEGORY_UUID, "Local Machine", new LocalStore());
        e.setDirectory(getStoresDir().resolve(LOCAL_ID.toString()));
        e.setConfiguration(
                StorageElement.Configuration.builder().deletable(false).build());
        storeEntries.put(e, e);
        e.validate();
    }

    @Override
    public synchronized void save(boolean dispose) {
        var storesDir = getStoresDir();

        TrackEvent.info("Storage persistence is disabled. Deleting storage contents ...");
        try {
            if (Files.exists(storesDir)) {
                FileUtils.cleanDirectory(storesDir.toFile());
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).build().handle();
        }
    }

    @Override
    public boolean supportsSharing() {
        return false;
    }
}
