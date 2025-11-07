package io.xpipe.app.util;

import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class LocalFileTracker {

    private static final Set<Path> localFiles = new HashSet<>();

    public static void deleteOnExit(Path file) {
        synchronized (localFiles) {
            localFiles.add(file);
        }
    }

    public static void reset() {
        synchronized (localFiles) {
            for (Path localFile : localFiles) {
                FileUtils.deleteQuietly(localFile.toFile());
            }
        }
    }
}
