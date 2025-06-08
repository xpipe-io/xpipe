package io.xpipe.app.browser.action;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuBranchProvider;
import io.xpipe.app.browser.menu.BrowserMenuItemProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;

import java.util.List;

public class BrowserActionProviders {


    public static BrowserActionProvider forClass(Class<? extends BrowserActionProvider> clazz) {
        return (BrowserActionProvider) ActionProvider.ALL.stream().filter(actionProvider -> actionProvider.getClass().equals(clazz)).findFirst().orElseThrow();
    }
}
