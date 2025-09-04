package io.xpipe.app.browser.menu;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;

import java.util.List;

public class BrowserMenuProviders {

    public static List<BrowserMenuLeafProvider> getFlattened(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return ActionProvider.ALL.stream()
                .map(browserAction -> browserAction instanceof BrowserMenuItemProvider ba
                        ? getFlattened(ba, model, entries)
                        : List.<BrowserMenuLeafProvider>of())
                .flatMap(List::stream)
                .toList();
    }

    public static List<BrowserMenuLeafProvider> getFlattened(
            BrowserMenuItemProvider browserAction, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return browserAction instanceof BrowserMenuLeafProvider
                ? List.of((BrowserMenuLeafProvider) browserAction)
                : browserAction.isApplicable(model, entries)
                        ? ((BrowserMenuBranchProvider) browserAction)
                                .getBranchingActions(model, entries).stream()
                                        .map(action -> getFlattened(action, model, entries))
                                        .flatMap(List::stream)
                                        .toList()
                        : List.of();
    }

    public static BrowserMenuLeafProvider byId(String id, BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return getFlattened(model, entries).stream()
                .filter(browserAction -> id.equals(browserAction.getId()))
                .findAny()
                .orElseThrow();
    }
}
