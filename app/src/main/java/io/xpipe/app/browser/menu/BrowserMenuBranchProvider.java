package io.xpipe.app.browser.menu;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.util.LicenseProvider;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public interface BrowserMenuBranchProvider extends BrowserMenuItemProvider {

    default MenuItem toMenuItem(BrowserFileSystemTabModel model, List<BrowserEntry> selected) {
        var m = new Menu(getName(model, selected).getValue() + " ...");
        for (var sub : getBranchingActions(model, selected)) {
            var subselected = resolveFilesIfNeeded(selected);
            if (!sub.isApplicable(model, subselected)) {
                continue;
            }
            var item = sub.toMenuItem(model, subselected);
            if (item != null) {
                m.getItems().add(item);
            }
        }

        if (m.getItems().isEmpty()) {
            return null;
        }

        var graphic = getIcon(model, selected);
        if (graphic != null) {
            m.setGraphic(graphic.createGraphicNode());
        }
        m.setDisable(!isActive(model, selected));

        return m;
    }

    List<? extends BrowserMenuItemProvider> getBranchingActions(
            BrowserFileSystemTabModel model, List<BrowserEntry> entries);
}
