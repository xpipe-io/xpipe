package io.xpipe.app.storage;

import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.util.regex.Matcher;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Value
public class ContextualFileReference {

    private static FilePath lastDataDir;

    @NonNull
    String path;

    private static FilePath getDataDir() {
        if (DataStorage.get() == null) {
            return lastDataDir != null
                    ? lastDataDir
                    : FilePath.of(AppProperties.get()
                                    .getDataDir()
                                    .resolve("storage")
                                    .resolve("data"))
                            .toUnix();
        }

        return lastDataDir = FilePath.of(DataStorage.get().getDataDir()).toUnix();
    }

    public static ContextualFileReference of(FilePath p) {
        if (p == null) {
            return null;
        }

        var ns = p.normalize().toUnix();
        var home =
                FilePath.of(AppSystemInfo.ofCurrent().getUserHome()).normalize().toUnix();

        String replaced;
        var withHomeResolved = ns.toString().replace("~", home.toString());
        // Only replace ~ if it is part of data dir, otherwise keep it raw
        if (withHomeResolved.startsWith(getDataDir().toString())) {
            replaced = withHomeResolved.replace("<DATA>", getDataDir().toString());
        } else {
            replaced = ns.toString().replace("<DATA>", getDataDir().toString());
        }
        return new ContextualFileReference(replaced);
    }

    public static ContextualFileReference of(String s) {
        return of(s != null ? FilePath.of(s) : null);
    }

    public FilePath toAbsoluteFilePath(ShellControl sc) {
        return FilePath.of(path.replaceAll(
                "/",
                Matcher.quoteReplacement(
                        sc != null ? OsFileSystem.of(sc.getOsType()).getFileSystemSeparator() : "/")));
    }

    public FilePath toLocalAbsoluteFilePath() {
        return FilePath.of(path.replaceAll(
                "/", Matcher.quoteReplacement(OsFileSystem.ofLocal().getFileSystemSeparator())));
    }

    public boolean isInDataDirectory() {
        return serialize().contains("<DATA>");
    }

    public String serialize() {
        var start = getDataDir();
        var normalizedPath = FilePath.of(path).normalize().toUnix();
        if (normalizedPath.startsWith(start) && !normalizedPath.equals(start)) {
            return "<DATA>" + "/" + normalizedPath.relativize(start);
        }
        return path;
    }
}
