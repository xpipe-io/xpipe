package io.xpipe.app.storage;

public interface DataStorageListener {

    void onStoreListUpdate();

    void onStoreAdd(DataStoreEntry... entry);

    void onStoreRemove(DataStoreEntry... entry);

    void onCategoryAdd(DataStoreCategory category);

    void onCategoryRemove(DataStoreCategory category);

    void onEntryCategoryChange();
}
