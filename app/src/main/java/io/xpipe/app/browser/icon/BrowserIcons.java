package io.xpipe.app.browser.icon;

import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.SimpleStringProperty;

public class BrowserIcons {
    public static PrettyImageComp createDefaultFileIcon() {
        return new PrettyImageComp(new SimpleStringProperty("default_file.svg"), 22, 22);
    }
    public static PrettyImageComp createDefaultDirectoryIcon() {
        return new PrettyImageComp(new SimpleStringProperty("default_folder.svg"), 22, 22);
    }
    public static PrettyImageComp createIcon(FileType type) {
        return new PrettyImageComp(new SimpleStringProperty(type.getIcon()), 22, 22);
    }

    public static PrettyImageComp createIcon(FileSystem.FileEntry entry) {
        return new PrettyImageComp(new SimpleStringProperty(FileIconManager.getFileIcon(entry, false)), 22, 22);
    }
}
