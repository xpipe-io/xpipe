package io.xpipe.app.storage;

import io.xpipe.app.ext.ProcessControlProvider;

import java.nio.file.Path;
import java.util.List;

public interface DataStorageSyncHandler {

    static DataStorageSyncHandler getInstance() {
        return (DataStorageSyncHandler) ProcessControlProvider.get().getStorageSyncHandler();
    }

    void reset() throws Exception;

    boolean validateConnection();

    boolean supportsSync();

    boolean hasExternalStoredCredentials();

    void init();

    void prepareGpgIfNeeded();

    void retrieveSyncedData();

    void refreshRemoteData();

    void afterStorageLoad();

    void beforeStorageSave();

    void afterStorageSave();

    void handleEntry(DataStoreEntry entry, boolean exists, boolean dirty);

    void handleCategory(DataStoreCategory category, boolean exists, boolean dirty);

    void handleDeletion(Path target, String name);

    Path getDirectory();

    List<Path> getSavedDataFiles();

    Path getDataFile(Path rel);

    Path addDataFile(Path file, Path target, boolean perUser);
}
