package io.xpipe.app.browser.file;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.util.InputHelper;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;

import java.util.ArrayList;
import java.util.List;

public final class BrowserContextMenu extends ContextMenu {

    private final OpenFileSystemModel model;
    private final BrowserEntry source;
    private final boolean quickAccess;

    public BrowserContextMenu(OpenFileSystemModel model, BrowserEntry source, boolean quickAccess) {
        this.model = model;
        this.source = source;
        this.quickAccess = quickAccess;
        createMenu();
    }

    private void createMenu() {
        InputHelper.onLeft(this, false, e -> {
            hide();
            e.consume();
        });

        AppFont.normal(this.getStyleableNode());

        var empty = source == null;
        var selected = new ArrayList<>(
                empty
                        ? List.of(new BrowserEntry(model.getCurrentDirectory(), model.getFileList()))
                        : quickAccess ? List.of() : model.getFileList().getSelection());
        if (source != null && !selected.contains(source)) {
            selected.add(source);
        }

        for (BrowserAction.Category cat : BrowserAction.Category.values()) {
            var all = BrowserAction.ALL.stream()
                    .filter(browserAction -> browserAction.getCategory() == cat)
                    .filter(browserAction -> {
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

            for (BrowserAction a : all) {
                var used = a.resolveFilesIfNeeded(selected);
                getItems().add(a.toMenuItem(model, used));
            }
        }
    }
}
