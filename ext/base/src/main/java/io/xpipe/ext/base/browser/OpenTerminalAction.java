package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.file.BrowserTerminalDockTabModel;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenTerminalAction implements BrowserLeafAction {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (entries.size() == 0) {
            model.openTerminalAsync(
                    model.getCurrentDirectory() != null
                            ? model.getCurrentDirectory().getPath()
                            : null);
        } else {
            for (var entry : entries) {
                model.openTerminalAsync(entry.getRawFileEntry().getPath());
            }
        }

        if (AppPrefs.get().enableTerminalDocking().get() && model.getBrowserModel() instanceof BrowserFullSessionModel sessionModel) {
            // Check if the right side is already occupied
            var existingSplit = sessionModel.getSplits().get(model);
            if (existingSplit != null && !(existingSplit instanceof BrowserTerminalDockTabModel)) {
                return;
            }

            sessionModel.splitTab(model, new BrowserTerminalDockTabModel(sessionModel, model, model.getTerminalRequests()));
        }
    }

    public String getId() {
        return "openTerminal";
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2c-console");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("openInTerminal");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var t = AppPrefs.get().terminalType().getValue();
        return t != null;
    }
}
