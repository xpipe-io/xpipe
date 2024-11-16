package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.icon.BrowserIcons;

import javafx.scene.Node;

import java.util.List;

public interface FileTypeAction extends BrowserAction {

    @Override
    default Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return BrowserIcons.createIcon(getType()).createRegion();
    }

    @Override
    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var t = getType();
        return entries.stream().allMatch(entry -> t.matches(entry.getRawFileEntry()));
    }

    BrowserIconFileType getType();
}
