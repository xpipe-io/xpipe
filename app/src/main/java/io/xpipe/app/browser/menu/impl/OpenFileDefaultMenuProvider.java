package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.action.impl.OpenDirectoryActionProvider;
import io.xpipe.app.browser.action.impl.OpenFileDefaultActionProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileOpener;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenFileDefaultMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public Class<? extends BrowserActionProvider> getDelegateActionClass() {
        return OpenFileDefaultActionProvider.class;
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2b-book-open-variant");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.ENTER);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("openWithDefaultApplication");
    }
}
