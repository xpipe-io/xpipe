package io.xpipe.app.browser.icon;

import io.xpipe.app.fxcomps.impl.PrettyImageComp;
import io.xpipe.core.store.FileSystem;
import javafx.beans.property.SimpleStringProperty;

public class FileIcons {

    public static PrettyImageComp createIcon(FileSystem.FileEntry entry) {
        return new PrettyImageComp(new SimpleStringProperty(FileIconManager.getFileIcon(entry, false)), 22, 22);
    }
}
