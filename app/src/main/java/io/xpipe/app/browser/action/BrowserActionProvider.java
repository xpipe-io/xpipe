package io.xpipe.app.browser.action;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.action.BrowserAction;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;

import io.xpipe.app.storage.DataStoreEntryRef;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import lombok.SneakyThrows;

import java.util.List;

public interface BrowserActionProvider extends ActionProvider {

    default boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }

    default boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return true;
    }
}
