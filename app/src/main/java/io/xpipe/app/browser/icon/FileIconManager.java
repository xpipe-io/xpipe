package io.xpipe.app.browser.icon;

import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;

public class FileIconManager {

    private static boolean loaded;

    public static synchronized void loadIfNecessary() {
        if (!loaded) {
            BrowserIconFileType.loadDefinitions();
            BrowserIconDirectoryType.loadDefinitions();
            loaded = true;
        }
    }

    public static synchronized String getFileIcon(FileEntry entry) {
        if (entry == null) {
            return null;
        }

        var r = entry.resolved();
        if (r.getKind() != FileKind.DIRECTORY) {
            for (var f : BrowserIconFileType.getAll()) {
                if (f.matches(r)) {
                    return f.getIcon();
                }
            }
        } else {
            for (var f : BrowserIconDirectoryType.getAll()) {
                if (f.matches(r)) {
                    return f.getIcon(r);
                }
            }
        }

        return "browser/" + (r.getKind() == FileKind.DIRECTORY ? "default_folder.svg" : "default_file.svg");
    }
}
