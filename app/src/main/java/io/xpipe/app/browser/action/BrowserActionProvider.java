package io.xpipe.app.browser.action;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;

import java.util.List;

public interface BrowserActionProvider extends ActionProvider {

    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }

    default boolean isActive() {
        return true;
    }
}
