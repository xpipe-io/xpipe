package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.browser.icon.FileType;
import javafx.scene.Node;

import java.util.List;

public interface FileTypeAction extends BrowserAction {

    @Override
    default Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return BrowserIcons.createIcon(getType()).createRegion();
    }

    @Override
    default boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var t = getType();
        return entries.stream().allMatch(entry -> t.matches(entry.getRawFileEntry()));
    }

    FileType getType();
}
