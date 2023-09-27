package io.xpipe.app.storage;

import io.xpipe.app.util.FeatureProvider;

import java.nio.file.Path;

public interface GitStorageHandler {

    static GitStorageHandler getInstance() {
        return FeatureProvider.get().createStorageHandler();
    }

    boolean supportsShare();

    void init(Path dir);

    void prepareForLoad();

    void prepareForSave();

    void handleEntry(DataStoreEntry entry, boolean exists, boolean dirty);

    void handleCategory(DataStoreCategory category, boolean exists, boolean dirty);

    void handleDeletion(Path target, String name);

    void postSave();
}
