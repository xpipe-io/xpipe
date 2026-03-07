package io.xpipe.app.util;

import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class LocalFileTracker {

    private static final Set<Path> localFiles = new HashSet<>();

    public static void deleteOnExit(Path file) {
        synchronized (localFiles) {
            localFiles.add(file);
        }

        GlobalTimer.scheduleUntil(Duration.ofHours(1), false, () -> {
            synchronized (localFiles) {
                var copy = new HashSet<>(localFiles);
                GlobalTimer.delay(() -> {
                    for (Path localFile : copy) {
                        FileUtils.deleteQuietly(localFile.toFile());
                    }
                }, Duration.ofMinutes(1));
            }
            return false;
        });
    }

    public static void reset() {
        synchronized (localFiles) {
            for (Path localFile : localFiles) {
                FileUtils.deleteQuietly(localFile.toFile());
            }
        }
    }
}
