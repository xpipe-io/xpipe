package io.xpipe.app.browser.file;

import io.xpipe.app.browser.action.BranchAction;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.util.LicenseProvider;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public final class BrowserContextMenu extends ContextMenu {

    private final OpenFileSystemModel model;
    private final BrowserEntry source;

    public BrowserContextMenu(OpenFileSystemModel model, BrowserEntry source) {
        this.model = model;
        this.source = source;
        createMenu();
    }

    private static List<BrowserEntry> resolveIfNeeded(BrowserAction action, List<BrowserEntry> selected) {
        return action.automaticallyResolveLinks()
                ? selected.stream()
                        .map(browserEntry -> new BrowserEntry(
                                browserEntry.getRawFileEntry().resolved(),
                                browserEntry.getModel()))
                        .toList()
                : selected;
    }

    private void createMenu() {
        AppFont.normal(this.getStyleableNode());

        var empty = source == null;
        var selected = new ArrayList<>(
                empty
                        ? List.of(new BrowserEntry(model.getCurrentDirectory(), model.getFileList()))
                        : model.getFileList().getSelection());
        if (source != null && !selected.contains(source)) {
            selected.add(source);
        }

        for (BrowserAction.Category cat : BrowserAction.Category.values()) {
            var all = BrowserAction.ALL.stream()
                    .filter(browserAction -> browserAction.getCategory() == cat)
                    .filter(browserAction -> {
                        var used = resolveIfNeeded(browserAction, selected);
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
                var used = resolveIfNeeded(a, selected);
                if (a instanceof LeafAction la) {
                    getItems().add(la.toMenuItem(model, used));
                }

                if (a instanceof BranchAction la) {
                    var m = new Menu(a.getName(model, used).getValue() + " ...");
                    for (LeafAction sub : la.getBranchingActions(model, used)) {
                        var subUsed = resolveIfNeeded(sub, selected);
                        if (!sub.isApplicable(model, subUsed)) {
                            continue;
                        }
                        m.getItems().add(sub.toMenuItem(model, subUsed));
                    }
                    var graphic = a.getIcon(model, used);
                    if (graphic != null) {
                        m.setGraphic(graphic);
                    }
                    m.setDisable(!a.isActive(model, used));

                    if (la.getProFeatureId() != null
                            && !LicenseProvider.get()
                                    .getFeature(la.getProFeatureId())
                                    .isSupported()) {
                        m.setDisable(true);
                        m.setGraphic(new FontIcon("mdi2p-professional-hexagon"));
                    }

                    getItems().add(m);
                }
            }
        }
    }
}
