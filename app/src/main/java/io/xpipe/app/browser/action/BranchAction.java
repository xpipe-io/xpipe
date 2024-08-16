package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.util.LicenseProvider;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public interface BranchAction extends BrowserAction {

    default MenuItem toMenuItem(OpenFileSystemModel model, List<BrowserEntry> selected) {
        var m = new Menu(getName(model, selected).getValue() + " ...");
        for (var sub : getBranchingActions(model, selected)) {
            var subselected = resolveFilesIfNeeded(selected);
            if (!sub.isApplicable(model, subselected)) {
                continue;
            }
            m.getItems().add(sub.toMenuItem(model, subselected));
        }
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            m.setGraphic(graphic);
        }
        m.setDisable(!isActive(model, selected));

        if (getProFeatureId() != null
                && !LicenseProvider.get().getFeature(getProFeatureId()).isSupported()) {
            m.setDisable(true);
            m.setGraphic(new FontIcon("mdi2p-professional-hexagon"));
        }

        return m;
    }

    List<? extends BrowserAction> getBranchingActions(OpenFileSystemModel model, List<BrowserEntry> entries);
}
