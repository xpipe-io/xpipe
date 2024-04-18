package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.fxcomps.impl.TooltipAugment;
import io.xpipe.app.fxcomps.util.Shortcuts;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.ThreadHelper;

import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public interface LeafAction extends BrowserAction {

    void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception;

    default Button toButton(OpenFileSystemModel model, List<BrowserEntry> selected) {
        var b = new Button();
        b.setOnAction(event -> {
            // Only accept shortcut actions in the current tab
            if (!model.equals(model.getBrowserModel().getSelectedEntry().getValue())) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.execute(model.getBusy(), () -> {
                    // Start shell in case we exited
                    model.getFileSystem().getShell().orElseThrow().start();
                    execute(model, selected);
                });
            });
            event.consume();
        });
        if (getShortcut() != null) {
            Shortcuts.addShortcut(b, getShortcut());
        }
        var name = getName(model, selected);
        new TooltipAugment<>(name).augment(b);
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            b.setGraphic(graphic);
        }
        b.setMnemonicParsing(false);
        b.accessibleTextProperty().bind(name);

        b.setDisable(!isActive(model, selected));
        model.getCurrentPath().addListener((observable, oldValue, newValue) -> {
            b.setDisable(!isActive(model, selected));
        });

        if (getProFeatureId() != null
                && !LicenseProvider.get().getFeature(getProFeatureId()).isSupported()) {
            b.setDisable(true);
            b.setGraphic(new FontIcon("mdi2p-professional-hexagon"));
        }

        return b;
    }

    default MenuItem toMenuItem(OpenFileSystemModel model, List<BrowserEntry> selected) {
        var name = getName(model, selected);
        var mi = new MenuItem();
        mi.textProperty().bind(name);
        mi.setOnAction(event -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.execute(model.getBusy(), () -> {
                    // Start shell in case we exited
                    model.getFileSystem().getShell().orElseThrow().start();
                    execute(model, selected);
                });
            });
            event.consume();
        });
        if (getShortcut() != null) {
            mi.setAccelerator(getShortcut());
        }
        var graphic = getIcon(model, selected);
        if (graphic != null) {
            mi.setGraphic(graphic);
        }
        mi.setMnemonicParsing(false);
        mi.setDisable(!isActive(model, selected));

        if (getProFeatureId() != null
                && !LicenseProvider.get().getFeature(getProFeatureId()).isSupported()) {
            mi.setDisable(true);
            mi.setText(mi.getText() + " (Pro)");
        }

        return mi;
    }

    default String getId() {
        return null;
    }
}
