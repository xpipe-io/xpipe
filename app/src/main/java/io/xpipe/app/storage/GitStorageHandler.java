package io.xpipe.app.storage;

import io.xpipe.app.util.LicenseProvider;

import java.nio.file.Path;

public interface GitStorageHandler {

    static GitStorageHandler getInstance() {
        return LicenseProvider.get().createStorageHandler();
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
