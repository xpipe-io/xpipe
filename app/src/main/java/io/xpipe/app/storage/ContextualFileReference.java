package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Matcher;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContextualFileReference {

    private static String lastDataDir;

    private static String getDataDir() {
        if (DataStorage.get() == null) {
            return lastDataDir != null ? lastDataDir : normalized(AppPrefs.DEFAULT_STORAGE_DIR);
        }

        return lastDataDir = normalized(DataStorage.get().getDataDir());
    }

    private static String normalized(Path p) {
        return p.normalize().toString().replaceAll("\\\\", "/");
    }

    private static String normalized(String s) {
        try {
            return Path.of(s).normalize().toString().replaceAll("\\\\", "/");
        } catch (InvalidPathException ex) {
            return s;
        }
    }

    public static ContextualFileReference of(String s) {
        if (s == null) {
            return null;
        }

        var ns = normalized(s.trim());
        // Replacement order is important
        var replaced = ns.replace("<DATA>", getDataDir())
                .replace("~", normalized(System.getProperty("user.home")));
        return new ContextualFileReference(normalized(replaced));
    }

    @NonNull
    private final String path;

    public String toString() {
        return path.replaceAll("/", Matcher.quoteReplacement(OsType.getLocal().getFileSystemSeparator()));
    }

    public String serialize() {
        var start = getDataDir();
        var normalizedPath = normalized(path);
        if (normalizedPath.startsWith(start)) {
            return "<DATA>" + "/" + FileNames.relativize(start, normalizedPath);
        }
        return path;
    }
}
