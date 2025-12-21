package io.xpipe.app.browser.file;

import io.xpipe.app.ext.FileSystem;
import io.xpipe.core.FilePath;

import java.util.regex.Pattern;

public class BrowserFileDuplicates {

    public static FilePath renameFileDuplicate(FileSystem fileSystem, FilePath target, boolean dir) throws Exception {
        // Who has more than 10 copies?
        for (int i = 0; i < 10; i++) {
            target = renameFile(target, dir);
            if ((dir && !fileSystem.directoryExists(target)) || (!dir && !fileSystem.fileExists(target))) {
                return target;
            }
        }
        return target;
    }

    private static FilePath renameFile(FilePath target, boolean dir) {
        var name = dir ? target.getFileName() : target.getBaseName().getFileName();
        var pattern = Pattern.compile("(.+)_(\\d+)");
        var matcher = pattern.matcher(name);
        if (matcher.matches()) {
            try {
                var number = Integer.parseInt(matcher.group(2));
                var suffix = dir ? "" : target.getExtension().map(s -> "." + s).orElse("");
                var newFile = target.getParent().join(matcher.group(1) + "_" + (number + 1) + suffix);
                return newFile;
            } catch (NumberFormatException ignored) {
            }
        }

        var ext = target.getExtension();
        return FilePath.of(target.removeTrailingSlash().getBaseName() + "_" + 1 + (ext.isPresent() ? "." + ext.get() : ""));
    }
}
