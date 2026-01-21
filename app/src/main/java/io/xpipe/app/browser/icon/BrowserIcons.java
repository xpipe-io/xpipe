package io.xpipe.app.browser.icon;


import io.xpipe.app.comp.base.PrettyImageHelper;
import io.xpipe.app.ext.FileEntry;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

public class BrowserIcons {

    public static BaseRegionBuilder<?,?> createDefaultFileIcon() {
        var s = "browser/default_file.svg";
        BrowserIconManager.loadIfNecessary(s);
        return PrettyImageHelper.ofFixedSizeSquare(s, 24);
    }

    public static BaseRegionBuilder<?,?> createDefaultDirectoryIcon() {
        var s = "browser/default_folder.svg";
        BrowserIconManager.loadIfNecessary(s);
        return PrettyImageHelper.ofFixedSizeSquare(s, 24);
    }

    public static BaseRegionBuilder<?,?> createContextMenuIcon(BrowserIconFileType type) {
        BrowserIconManager.loadIfNecessary(type.getIcon());
        return PrettyImageHelper.ofFixedSizeSquare(type.getIcon(), 16);
    }

    public static BaseRegionBuilder<?,?> createIcon(String s) {
        BrowserIconManager.loadIfNecessary(s);
        return PrettyImageHelper.ofFixedSizeSquare(s, 24);
    }
}
