package io.xpipe.app.storage;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;

public class ImpersistentStorage extends DataStorage {

    @Override
    public void load() {
    }

    @Override
    public boolean supportsSharing() {
        return false;
    }

    @Override
    public void save() {
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

}
