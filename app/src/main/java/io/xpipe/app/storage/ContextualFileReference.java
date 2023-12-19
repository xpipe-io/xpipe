package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.ShellControl;
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

        String replaced;
        var withHomeResolved = ns.replace("~", normalized(System.getProperty("user.home")));
        // Only replace ~ if it is part of data dir, otherwise keep it raw
        if (withHomeResolved.startsWith(getDataDir())) {
            replaced = withHomeResolved.replace("<DATA>", getDataDir());
        } else {
            replaced = ns.replace("<DATA>", getDataDir());
        }
        return new ContextualFileReference(normalized(replaced));
    }

    @NonNull
    private final String path;

    public String toFilePath(ShellControl sc) {
        return path.replaceAll("/", Matcher.quoteReplacement(sc != null ? sc.getOsType().getFileSystemSeparator() : "/"));
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
