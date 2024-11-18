package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.store.FileKind;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Collections;
import java.util.List;

public class DownloadAction implements BrowserLeafAction {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var transfer = model.getBrowserModel();
        if (!(transfer instanceof BrowserFullSessionModel fullSessionModel)) {
            return;
        }

        fullSessionModel.getLocalTransfersStage().drop(model, entries);
    }

    public String getId() {
        return "download";
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2d-download");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("download");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var transfer = model.getBrowserModel();
        if (!(transfer instanceof BrowserFullSessionModel)) {
            return false;
        }
        return true;
    }
}
