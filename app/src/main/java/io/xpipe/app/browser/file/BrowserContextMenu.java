package io.xpipe.app.browser.file;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuItemProvider;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.platform.InputHelper;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import java.util.ArrayList;
import java.util.List;

public final class BrowserContextMenu extends ContextMenu {

    private final BrowserFileSystemTabModel model;
    private final BrowserEntry source;
    private final boolean quickAccess;

    public BrowserContextMenu(BrowserFileSystemTabModel model, BrowserEntry source, boolean quickAccess) {
        this.model = model;
        this.source = source;
        this.quickAccess = quickAccess;
        createMenu();
    }

    private void createMenu() {
        AppFontSizes.lg(getStyleableNode());

        InputHelper.onLeft(this, false, e -> {
            hide();
            e.consume();
        });

        var empty = source == null;
        var selected = new ArrayList<>(
                empty
                        ? List.of(new BrowserEntry(model.getCurrentDirectory(), model.getFileList()))
                        : quickAccess ? List.of() : model.getFileList().getSelection());
        if (source != null && !selected.contains(source)) {
            selected.add(source);
        }

        if (model.isClosed()) {
            return;
        }

        for (var cat : BrowserMenuCategory.values()) {
            var all = ActionProvider.ALL.stream()
                    .map(actionProvider -> actionProvider instanceof BrowserMenuItemProvider ba ? ba : null)
                    .filter(browserActionProvider -> browserActionProvider != null)
                    .filter(browserAction -> browserAction.getCategory() == cat)
                    .filter(browserAction -> {
                        if (model.isClosed()) {
                            return false;
                        }

                        var used = browserAction.resolveFilesIfNeeded(selected);
                        if (!browserAction.isApplicable(model, used)) {
                            return false;
                        }

                        if (!browserAction.acceptsEmptySelection() && empty) {
                            return false;
                        }

                        return true;
                    })
                    .toList();
            if (all.size() == 0) {
                continue;
            }

            if (getItems().size() > 0) {
                getItems().add(new SeparatorMenuItem());
            }

            for (var a : all) {
                if (model.isClosed()) {
                    return;
                }

                var used = a.resolveFilesIfNeeded(selected);
                var item = a.toMenuItem(model, used);
                if (item != null) {
                    getItems().add(item);
                }
            }
        }
    }
}
