package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserAlerts;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemHelper;
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

public class DeleteAction implements BrowserLeafAction {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
        var toDelete = entries.stream().map(entry -> entry.getRawFileEntry()).toList();
        if (!BrowserAlerts.showDeleteAlert(model, toDelete)) {
            return;
        }

        BrowserFileSystemHelper.delete(toDelete);
        model.refreshSync();
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2d-delete");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.DELETE);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable(
                "deleteFile",
                entries.stream()
                                .allMatch(browserEntry ->
                                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK)
                        ? " link"
                        : "");
    }
}
