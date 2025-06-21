package io.xpipe.app.beacon;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.ShellTemp;
import io.xpipe.beacon.BeaconClientException;

import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlobManager {

    private static final Path TEMP = ShellTemp.getLocalTempDataDirectory("blob");
    private static BlobManager INSTANCE;
    private final Map<UUID, byte[]> memoryBlobs = new ConcurrentHashMap<>();
    private final Map<UUID, Path> fileBlobs = new ConcurrentHashMap<>();

    public static BlobManager get() {
        return INSTANCE;
    }

    public static void init() {
        INSTANCE = new BlobManager();
        try {
            FileUtils.forceMkdir(TEMP.toFile());
            try {
                // Remove old files in dir
                FileUtils.cleanDirectory(TEMP.toFile());
            } catch (IOException ignored) {
            }
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    public static void reset() {
        try {
            FileUtils.cleanDirectory(TEMP.toFile());
        } catch (IOException ignored) {
        }
        INSTANCE = null;
    }

    public Path newBlobFile() throws IOException {
        var file = TEMP.resolve(UUID.randomUUID().toString());
        FileUtils.forceMkdir(file.getParent().toFile());
        return file;
    }

    public void store(UUID uuid, byte[] blob) {
        memoryBlobs.put(uuid, blob);
    }

    public void store(UUID uuid, InputStream blob) throws IOException {
        var file = TEMP.resolve(uuid.toString());
        try (var fileOut = Files.newOutputStream(file)) {
            blob.transferTo(fileOut);
        }
        fileBlobs.put(uuid, file);
    }

    public InputStream getBlob(UUID uuid) throws Exception {
        var memory = memoryBlobs.get(uuid);
        if (memory != null) {
            return new ByteArrayInputStream(memory);
        }

        var found = fileBlobs.get(uuid);
        if (found == null) {
            throw new BeaconClientException("No saved data known for id " + uuid);
        }

        return Files.newInputStream(found);
    }
}
