package io.xpipe.app.storage;

import io.xpipe.core.process.ProcessControlProvider;

import java.nio.file.Path;

public interface GitStorageHandler {

    static GitStorageHandler getInstance() {
        return (GitStorageHandler) ProcessControlProvider.get().createStorageHandler();
    }

    void onReset();

    boolean supportsShare();

    void init(Path dir);

    void beforeStorageLoad();

    void afterStorageLoad();

    void beforeStorageSave();

    void afterStorageSave();

    void handleEntry(DataStoreEntry entry, boolean exists, boolean dirty);

    void handleCategory(DataStoreCategory category, boolean exists, boolean dirty);

    void handleDeletion(Path target, String name);
}
