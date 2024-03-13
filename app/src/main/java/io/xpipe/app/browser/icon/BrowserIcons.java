package io.xpipe.app.browser.icon;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.PrettyImageHelper;
import io.xpipe.core.store.FileSystem;

public class BrowserIcons {

    public static Comp<?> createDefaultFileIcon() {
        return PrettyImageHelper.ofFixedSizeSquare("default_file.svg", 22);
    }

    public static Comp<?> createDefaultDirectoryIcon() {
        return PrettyImageHelper.ofFixedSizeSquare("default_folder.svg", 22);
    }

    public static Comp<?> createIcon(BrowserIconFileType type) {
        return PrettyImageHelper.ofFixedSizeSquare(type.getIcon(), 22);
    }

    public static Comp<?> createIcon(FileSystem.FileEntry entry) {
        return PrettyImageHelper.ofFixedSizeSquare(FileIconManager.getFileIcon(entry, false), 22);
    }
}
