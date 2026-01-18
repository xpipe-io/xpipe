package io.xpipe.app.browser.icon;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.ext.FileEntry;

public class BrowserIcons {

    public static Comp<?> createDefaultFileIcon() {
        var s = "browser/default_file.svg";
        BrowserIconManager.loadIfNecessary(s);
        return PrettyImageHelper.ofFixedSizeSquare(s, 24);
    }

    public static Comp<?> createDefaultDirectoryIcon() {
        var s = "browser/default_folder.svg";
        BrowserIconManager.loadIfNecessary(s);
        return PrettyImageHelper.ofFixedSizeSquare(s, 24);
    }

    public static Comp<?> createContextMenuIcon(BrowserIconFileType type) {
        BrowserIconManager.loadIfNecessary(type.getIcon());
        return PrettyImageHelper.ofFixedSizeSquare(type.getIcon(), 16);
    }

    public static Comp<?> createIcon(String s) {
        BrowserIconManager.loadIfNecessary(s);
        return PrettyImageHelper.ofFixedSizeSquare(s, 24);
    }
}
