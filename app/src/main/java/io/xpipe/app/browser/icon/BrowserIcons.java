package io.xpipe.app.browser.icon;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.core.store.FileEntry;

public class BrowserIcons {

    public static Comp<?> createDefaultFileIcon() {
        return PrettyImageHelper.ofFixedSizeSquare("default_file.svg", 24);
    }

    public static Comp<?> createDefaultDirectoryIcon() {
        return PrettyImageHelper.ofFixedSizeSquare("default_folder.svg", 24);
    }

    public static Comp<?> createIcon(BrowserIconFileType type) {
        return PrettyImageHelper.ofFixedSizeSquare(type.getIcon(), 24);
    }

    public static Comp<?> createIcon(FileEntry entry) {
        return PrettyImageHelper.ofFixedSizeSquare(FileIconManager.getFileIcon(entry, false), 24);
    }
}
