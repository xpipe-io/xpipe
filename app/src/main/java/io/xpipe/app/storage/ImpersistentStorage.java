package io.xpipe.app.storage;

import io.xpipe.app.comp.store.StoreSortMode;
import io.xpipe.core.store.LocalStore;

import java.time.Instant;
import java.util.UUID;

public class ImpersistentStorage extends DataStorage {

    @Override
    public String getVaultKey() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void load() {
        {
            var cat = DataStoreCategory.createNew(null, ALL_CONNECTIONS_CATEGORY_UUID, "All connections");
            storeCategories.add(cat);
        }
        {
            var cat = DataStoreCategory.createNew(null, ALL_SCRIPTS_CATEGORY_UUID, "All scripts");
            storeCategories.add(cat);
        }
        {
            var cat = new DataStoreCategory(
                    null,
                    DEFAULT_CATEGORY_UUID,
                    "Default",
                    Instant.now(),
                    Instant.now(),
                    null,
                    true,
                    ALL_CONNECTIONS_CATEGORY_UUID,
                    StoreSortMode.getDefault(),
                    true,
                    true);
            storeCategories.add(cat);
            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
        }

        var e = DataStoreEntry.createNew(
                LOCAL_ID, DataStorage.DEFAULT_CATEGORY_UUID, "Local Machine", new LocalStore());
        e.setConfiguration(
                StorageElement.Configuration.builder().deletable(false).build());
        storeEntries.put(e, e);
        e.validate();
    }

    @Override
    public synchronized void save(boolean dispose) {}

    @Override
    public boolean supportsSharing() {
        return false;
    }
}
