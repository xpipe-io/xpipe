package io.xpipe.app.browser;

import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.SimpleStringProperty;

public class FileIcons {

    public static PrettyImageComp createIcon(FileSystem.FileEntry entry) {
        return new PrettyImageComp(new SimpleStringProperty(getIcon(entry)), 22, 22);
    }

    public static String getIcon(FileSystem.FileEntry entry) {
        if (!entry.isDirectory()) {
            return "app:file_drag_icon.png";
        } else {
            return "app:folder_closed.svg";
        }
    }
}
