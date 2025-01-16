package io.xpipe.app.storage;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileNames;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class ContextualFileReference {

    private static String lastDataDir;

    @NonNull
    String path;

    private static String getDataDir() {
        if (DataStorage.get() == null) {
            return lastDataDir != null ? lastDataDir : normalized(AppPrefs.DEFAULT_STORAGE_DIR.resolve("data"));
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

    public static Optional<ContextualFileReference> parseIfInDataDirectory(String s) {
        var cf = of(s);
        if (cf.serialize().contains("<DATA>")) {
            return Optional.of(cf);
        } else {
            return Optional.empty();
        }
    }

    public static Optional<String> resolveIfInDataDirectory(ShellControl shellControl, String s) {
        if (s.contains("<DATA>")) {
            var cf = of(s);
            return Optional.of(cf.toAbsoluteFilePath(shellControl));
        } else {
            return Optional.empty();
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

    public String toAbsoluteFilePath(ShellControl sc) {
        return path.replaceAll(
                "/", Matcher.quoteReplacement(sc != null ? sc.getOsType().getFileSystemSeparator() : "/"));
    }

    public boolean isInDataDirectory() {
        return serialize().contains("<DATA>");
    }

    public String serialize() {
        var start = getDataDir();
        var normalizedPath = normalized(path);
        if (normalizedPath.startsWith(start) && !normalizedPath.equals(start)) {
            return "<DATA>" + "/" + FileNames.relativize(start, normalizedPath);
        }
        return path;
    }
}
