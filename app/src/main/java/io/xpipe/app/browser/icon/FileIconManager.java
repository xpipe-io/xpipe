package io.xpipe.app.browser.icon;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.AppResources;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileSystem;

public class FileIconManager {

    private static boolean loaded;

    public static synchronized void loadIfNecessary() {
        if (!loaded) {
            AppImages.loadDirectory(AppResources.XPIPE_MODULE, "browser_icons");
            loaded = true;
        }
    }

    public static String getFileIcon(FileSystem.FileEntry entry, boolean open) {
        if (entry == null) {
            return null;
        }

        loadIfNecessary();

        var r = entry.resolved();
        if (r.getKind() != FileKind.DIRECTORY) {
            for (var f : BrowserIconFileType.ALL) {
                if (f.matches(r)) {
                    return getIconPath(f.getIcon());
                }
            }
        } else {
            for (var f : BrowserIconDirectoryType.ALL) {
                if (f.matches(r)) {
                    return getIconPath(f.getIcon(r, open));
                }
            }
        }

        return r.getKind() == FileKind.DIRECTORY
                ? (open ? "default_folder_opened.svg" : "default_folder.svg")
                : "default_file.svg";
    }

    private static String getIconPath(String name) {
        return name;
    }
}
