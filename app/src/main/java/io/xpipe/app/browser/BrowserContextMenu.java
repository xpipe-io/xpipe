package io.xpipe.app.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.core.AppFont;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import java.util.ArrayList;
import java.util.List;

final class BrowserContextMenu extends ContextMenu {

    private final OpenFileSystemModel model;
    private final BrowserEntry source;

    public BrowserContextMenu(OpenFileSystemModel model, BrowserEntry source) {
        this.model = model;
        this.source = source;
        createMenu();
    }

    private void createMenu() {
        AppFont.normal(this.getStyleableNode());

        var empty = source == null;
        var selected = new ArrayList<>(empty ? List.of(new BrowserEntry(model.getCurrentDirectory(), model.getFileList(), false)) : model.getFileList().getSelection());
        if (source != null && !selected.contains(source)) {
            selected.add(source);
        }

        for (BrowserAction.Category cat : BrowserAction.Category.values()) {
            var all = BrowserAction.ALL.stream()
                    .filter(browserAction -> browserAction.getCategory() == cat)
                    .filter(browserAction -> {
                        if (!browserAction.isApplicable(model, selected)) {
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
                if (a instanceof LeafAction la) {
                    getItems().add(la.toItem(model, selected, s -> s));
                }

                if (a instanceof BranchAction la) {
                    var m = new Menu(a.getName(model, selected) + " ...");
                    for (LeafAction sub : la.getBranchingActions()) {
                        if (!sub.isApplicable(model, selected)) {
                            continue;
                        }
                        m.getItems().add(sub.toItem(model, selected, s -> s));
                    }
                    var graphic = a.getIcon(model, selected);
                    if (graphic != null) {
                        m.setGraphic(graphic);
                    }
                    m.setDisable(!a.isActive(model, selected));
                    getItems().add(m);
                }
            }
        }
    }
}
