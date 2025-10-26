package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileKind;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.FilePath;

import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.Collections;
import java.util.List;

public class OpenTerminalInDirectoryMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var dirs = entries.size() > 0
                ? entries.stream()
                        .map(browserEntry -> browserEntry.getRawFileEntry().getPath())
                        .toList()
                : model.getCurrentDirectory() != null
                        ? List.of(model.getCurrentDirectory().getPath())
                        : Collections.singletonList((FilePath) null);
        for (var dir : dirs) {
            var name = (model.getFileSystem().supportsTerminalWorkingDirectory() && dir != null ? dir + " - " : "")
                    + model.getName().getValue();
            model.openTerminalAsync(
                    name, dir, model.getFileSystem().getRawShellControl().orElseThrow(), dirs.size() == 1);
        }
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }

    public String getId() {
        return "openInTerminal";
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2c-console");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.OPEN;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("openInTerminal");
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model) {
        var t = AppPrefs.get().terminalType().getValue();
        return t != null;
    }
}
