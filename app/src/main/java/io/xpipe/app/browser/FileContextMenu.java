/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.ThreadHelper;
import javafx.collections.FXCollections;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.List;
import java.util.function.UnaryOperator;

final class FileContextMenu extends ContextMenu {

    private final OpenFileSystemModel model;
    private final boolean empty;

    public FileContextMenu(OpenFileSystemModel model, boolean empty) {
        super();
        this.model = model;
        this.empty = empty;
        createMenu();
    }

    private void createMenu() {
        var selected = empty ? FXCollections.<FileBrowserEntry>observableArrayList() : model.getFileList().getSelected();

        for (BrowserAction.Category cat : BrowserAction.Category.values()) {
            var all = BrowserAction.ALL.stream().filter(browserAction -> browserAction.getCategory() == cat).filter(browserAction -> {
                if (!browserAction.isApplicable(model, selected)) {
                    return false;
                }

                if (!browserAction.acceptsEmptySelection() && selected.isEmpty()) {
                    return false;
                }

                return true;
            }).toList();
            if (all.size() == 0) {
                continue;
            }

            if (getItems().size() > 0) {
                getItems().add(new SeparatorMenuItem());
            }

            for (BrowserAction a : all) {
                if (a instanceof LeafAction la) {
                    getItems().add(toItem(la, selected, s -> s));
                }

                if (a instanceof BranchAction la) {
                    var m = new Menu(a.getName(model, selected) + " ...");
                    for (LeafAction sub : la.getBranchingActions()) {
                        if (!sub.isApplicable(model, selected)) {
                            continue;
                        }
                        m.getItems().add(toItem(sub, selected, s -> "... " + s));
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

    private MenuItem toItem(LeafAction a, List<FileBrowserEntry> selected, UnaryOperator<String> nameFunc) {
        var mi = new MenuItem(nameFunc.apply(a.getName(model, selected)));
        mi.setOnAction(event -> {
            ThreadHelper.runFailableAsync(() -> {
                BusyProperty.execute(model.getBusy(), () -> {
                a.execute(model, selected);
                });
            });
            event.consume();
        });
        if (a.getShortcut() != null) {
            mi.setAccelerator(a.getShortcut());
        }
        var graphic = a.getIcon(model, selected);
        if (graphic != null) {
            mi.setGraphic(graphic);
        }
        mi.setMnemonicParsing(false);
        mi.setDisable(!a.isActive(model, selected));
        return mi;
    }
}
