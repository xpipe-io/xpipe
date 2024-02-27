package io.xpipe.app.storage;

import io.xpipe.core.process.ProcessControlProvider;

import java.nio.file.Path;

public interface GitStorageHandler {

    static GitStorageHandler getInstance() {
        return (GitStorageHandler) ProcessControlProvider.get().getGitStorageHandler();
    }

    boolean supportsShare();

    void init();

    void setupRepositoryAndPull();

    void afterStorageLoad();

    void beforeStorageSave();

    void afterStorageSave();

    void handleEntry(DataStoreEntry entry, boolean exists, boolean dirty);

    void handleCategory(DataStoreCategory category, boolean exists, boolean dirty);

    void handleDeletion(Path target, String name);
}
