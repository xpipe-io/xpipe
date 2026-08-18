package io.xpipe.app.storage;

import io.xpipe.app.store.LocalStore;

import java.util.Optional;
import java.util.UUID;

public class ImpersistentStorage extends DataStorage {

    @Override
    public void pullManually() {}

    @Override
    public void pushManually() {}

    @Override
    public void reloadContent() {}

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
            var cat = DataStoreCategory.createNew(null, ALL_IDENTITIES_CATEGORY_UUID, "All identities");
            storeCategories.add(cat);
        }
        {
            var cat = DataStoreCategory.createNew(DEFAULT_CATEGORY_UUID, "Default");
            storeCategories.add(cat);
            selectedCategory = getStoreCategoryIfPresent(DEFAULT_CATEGORY_UUID).orElseThrow();
        }

        var e = DataStoreEntry.createNew(
                LOCAL_ID, DataStorage.DEFAULT_CATEGORY_UUID, "Local Machine", new LocalStore());
        storeEntries.put(e, e);
        e.validate();

        entriesAvailable = true;
    }

    @Override
    public void saveAsync() {}

    @Override
    public synchronized void save(boolean dispose, boolean forceSync) {}

    @Override
    public boolean syncEnabled() {
        return false;
    }

    @Override
    public Optional<DataStoreEntry> getInaccessibleEntry(UUID uuid) {
        return Optional.empty();
    }

    @Override
    protected void deleteStoreEntryFromDisk(DataStoreEntry entry) {}

    @Override
    protected void deleteStoreCategoryFromDisk(DataStoreCategory cat) {}
}
