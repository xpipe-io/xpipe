package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.file.BrowserClipboard;
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

public class CopyMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        BrowserClipboard.startCopy(model.getCurrentDirectory(), entries);
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2c-content-copy");
    }

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("copy");
    }
}
