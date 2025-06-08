package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.action.impl.OpenTerminalActionProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Collections;
import java.util.List;

public class OpenTerminalMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public Class<? extends BrowserActionProvider> getDelegateActionClass() {
        return OpenTerminalActionProvider.class;
    }

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
            var name = (dir != null ? dir + " - " : "") + model.getName().getValue();
            model.openTerminalAsync(name, dir, model.getFileSystem().getShell().orElseThrow(), dirs.size() == 1);
        }
    }

    public String getId() {
        return "openTerminal";
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2c-console");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
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
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }

    @Override
    public boolean isActive(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var t = AppPrefs.get().terminalType().getValue();
        return t != null;
    }
}
