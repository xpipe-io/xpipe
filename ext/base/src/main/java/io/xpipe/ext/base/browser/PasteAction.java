package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.file.BrowserClipboard;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileTransferMode;
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

public class PasteAction implements BrowserLeafAction {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var clipboard = BrowserClipboard.retrieveCopy();
        if (clipboard == null) {
            return;
        }

        var target = entries.size() == 1 && entries.getFirst().getRawFileEntry().getKind() == FileKind.DIRECTORY
                ? entries.getFirst().getRawFileEntry()
                : model.getCurrentDirectory();
        var files = clipboard.getEntries();
        if (files.size() == 0) {
            return;
        }

        model.dropFilesIntoAsync(
                target,
                files.stream()
                        .map(browserEntry -> browserEntry.getRawFileEntry())
                        .toList(),
                BrowserFileTransferMode.COPY);
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2c-content-paste");
    }

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("paste");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return (entries.size() == 1
                        && entries.stream()
                                .allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY))
                || entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return BrowserClipboard.retrieveCopy() != null;
    }
}
