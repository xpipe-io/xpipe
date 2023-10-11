package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class RefreshDirectoryAction implements LeafAction {

    public String getId() {
        return "refresh";
    }

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        model.refreshSync();
    }

    @Override
    public boolean isActive(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return !model.getInOverview().get();
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdmz-refresh");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return false;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.F5);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Refresh";
    }
}
