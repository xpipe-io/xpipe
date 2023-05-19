package io.xpipe.app.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.core.AppFont;
import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

final class BrowserContextMenu extends ContextMenu {

    private final OpenFileSystemModel model;
    private final boolean empty;

    public BrowserContextMenu(OpenFileSystemModel model, boolean empty) {
        super();
        this.model = model;
        this.empty = empty;
        createMenu();
    }

    private void createMenu() {
        AppFont.normal(this.getStyleableNode());

        var selected = empty || model.getFileList().getSelected().isEmpty()
                ? FXCollections.observableArrayList(
                        new BrowserEntry(model.getCurrentDirectory(), model.getFileList(), false))
                : model.getFileList().getSelected();

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
