package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.action.impl.BrowseInNativeManagerActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.core.OsType;

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
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return switch (OsType.ofLocal()) {
            case OsType.Windows ignored -> AppI18n.observable("browseInWindowsExplorer");
            case OsType.Linux ignored -> AppI18n.observable("browseInDefaultFileManager");
            case OsType.MacOs ignored -> AppI18n.observable("browseInFinder");
        };
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2f-folder-eye-outline");
    }
}
