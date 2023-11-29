package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Matcher;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalFileReference {

    private static Path lastDataDir;

    private static Path getDataDir() {
        if (DataStorage.get() == null) {
            return lastDataDir != null ? lastDataDir : AppPrefs.DEFAULT_STORAGE_DIR;
        }

        return lastDataDir = DataStorage.get().getDataDir();
    }

    public static LocalFileReference of(String s) {
        if (s == null) {
            return null;
        }

        // Replacement order is important
        var replaced = s.trim().replace("<DATA>", getDataDir().toString())
                .replace("~",System.getProperty("user.home"));
        try {
            var normalized = Path.of(replaced).normalize().toString().replaceAll("\\\\", "/");
            return new LocalFileReference(normalized);
        } catch (InvalidPathException ex) {
            return new LocalFileReference(replaced);
        }
    }

    @NonNull
    private final String path;

    public String toString() {
        return path.replaceAll("/", Matcher.quoteReplacement(OsType.getLocal().getFileSystemSeparator()));
    }

    public String serialize() {
        var start = getDataDir();
        try {
            var normalizedPath = Path.of(path);
            if (normalizedPath.startsWith(start)) {
                return "<DATA>" + "/" + start.relativize(normalizedPath);
            }
        } catch (InvalidPathException ignored) {}

        return path;
    }
}
