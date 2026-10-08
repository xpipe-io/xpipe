package io.xpipe.app.browser.file;

import io.xpipe.app.fs.FileSystem;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.FilePath;

import java.util.regex.Pattern;

public class BrowserFileDuplicates {

    public static FilePath renameFileDuplicate(FileSystem fileSystem, FilePath target, boolean dir) throws Exception {
        for (int i = 0; i < 50; i++) {
            target = renameFile(target, dir);
            // A file and a directory can't share a name, so check for both
            if (!fileSystem.fileExists(target) && !fileSystem.directoryExists(target)) {
                return target;
            }
        }

        // How tf has more than 50 duplicates anyway
        throw ErrorEventFactory.expected(
                new IllegalStateException("Unable to find a free name for a copy of " + target.getFileName()));
    }

    static FilePath renameFile(FilePath target, boolean dir) {
        var name = dir || target.isDotFile()
                ? target.getFileName()
                : target.getBaseName().getFileName();
        var pattern = Pattern.compile("(.+)_(\\d+)");
        var matcher = pattern.matcher(name);
        if (matcher.matches()) {
            try {
                var number = Integer.parseInt(matcher.group(2));
                var suffix = dir || target.isDotFile()
                        ? ""
                        : target.getExtension().map(s -> "." + s).orElse("");
                var newFile = target.getParent().join(matcher.group(1) + "_" + (number + 1) + suffix);
                return newFile;
            } catch (NumberFormatException ignored) {
            }
        }

        if (target.isDotFile()) {
            return FilePath.of(target.removeTrailingSlash() + "_" + 1);
        }

        var ext = target.getExtension();
        return FilePath.of(
                target.removeTrailingSlash().getBaseName() + "_" + 1 + (ext.isPresent() ? "." + ext.get() : ""));
    }
}
