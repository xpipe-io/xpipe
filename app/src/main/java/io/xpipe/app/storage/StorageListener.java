package io.xpipe.app.storage;

public interface StorageListener {

    void onStoreAdd(DataStoreEntry... entry);

    void onStoreRemove(DataStoreEntry... entry);


    void onCategoryAdd(DataStoreCategory category);

    void onCategoryRemove(DataStoreCategory category);
}
