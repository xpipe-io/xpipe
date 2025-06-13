package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.action.impl.BrowseInNativeManagerActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.OsType;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class BrowseInNativeManagerMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public Class<? extends BrowserActionProvider> getDelegateActionProvider() {
        return BrowseInNativeManagerActionProvider.class;
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.OPEN;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return switch (OsType.getLocal()) {
            case OsType.Windows windows -> AppI18n.observable("browseInWindowsExplorer");
            case OsType.Linux linux -> AppI18n.observable("browseInDefaultFileManager");
            case OsType.MacOs macOs -> AppI18n.observable("browseInFinder");
        };
    }
}
