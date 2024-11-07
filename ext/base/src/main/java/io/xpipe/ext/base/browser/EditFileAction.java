package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.file.BrowserFileOpener;
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

import java.util.List;

public class EditFileAction implements BrowserLeafAction {

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        for (BrowserEntry entry : entries) {
            BrowserFileOpener.openInTextEditor(model, entry.getRawFileEntry());
        }
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2p-pencil");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var e = AppPrefs.get().externalEditor().getValue();
        return AppI18n.observable(
                "editWithEditor", e != null ? e.toTranslatedString().getValue() : "?");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var e = AppPrefs.get().externalEditor().getValue();
        return e != null;
    }
}
