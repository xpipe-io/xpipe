package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.icon.FileIcons;
import io.xpipe.app.browser.icon.FileType;
import javafx.scene.Node;

import java.util.List;

public interface FileTypeAction extends BrowserAction {

    @Override
    default boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        var t = getType();
        return entries.stream().allMatch(entry -> t.matches(entry.getRawFileEntry()));
    }

    @Override
    default Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return FileIcons.createIcon(getType()).createRegion();
    }

    FileType getType();
}
