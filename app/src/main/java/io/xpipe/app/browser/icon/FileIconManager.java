package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;

public class FileIconManager {

    private static boolean loaded;

    public static synchronized void loadIfNecessary() {
        if (!loaded) {
            BrowserIconFileType.loadDefinitions();
            BrowserIconDirectoryType.loadDefinitions();
            AppImages.loadDirectory(AppResources.XPIPE_MODULE, "browser_icons", true, false);
            loaded = true;
        }
    }

    public static synchronized String getFileIcon(FileEntry entry, boolean open) {
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
                    return f.getIcon(r, open);
                }
            }
        }

        return r.getKind() == FileKind.DIRECTORY
                ? (open ? "default_folder_opened.svg" : "default_folder.svg")
                : "default_file.svg";
    }
}
