package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class RefreshDirectoryMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
        model.refreshSync();
    }

    public String getId() {
        return "refresh";
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdmz-refresh");
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.F5);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("refresh");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return false;
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return !model.getInOverview().get();
    }
}
