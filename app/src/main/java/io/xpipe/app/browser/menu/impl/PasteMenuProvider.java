package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserClipboard;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.file.BrowserFileTransferMode;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.ext.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;

public class PasteMenuProvider implements BrowserMenuLeafProvider {

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

        var isDuplication = files.size() == 1 && target.getPath().equals(files.getFirst().getRawFileEntry().getPath().getParent());
        if (isDuplication) {
            model.duplicateFile(files.getFirst().getRawFileEntry());
        } else {
            model.dropFilesIntoAsync(target, files.stream().map(browserEntry -> browserEntry.getRawFileEntry()).toList(),
                    BrowserFileTransferMode.COPY);
        }
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return (entries.size() == 1
                        && entries.stream()
                                .allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY))
                || entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-content-paste");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.COPY_PASTE;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("paste");
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model) {
        return BrowserClipboard.retrieveCopy() != null;
    }
}
