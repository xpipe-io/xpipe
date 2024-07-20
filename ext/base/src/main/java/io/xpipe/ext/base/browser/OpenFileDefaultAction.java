package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserFileOpener;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenFileDefaultAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
        for (var entry : entries) {
            BrowserFileOpener.openInDefaultApplication(model, entry.getRawFileEntry());
        }
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
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
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("openWithDefaultApplication");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getFileList().getEditing().getValue() == null && entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }
}
