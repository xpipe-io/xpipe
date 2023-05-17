package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.prefs.AppPrefs;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenTerminalAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        if (entries.size() == 0) {
            model.openTerminalAsync(model.getCurrentDirectory().getPath());
            return;
        }

        for (var entry : entries) {
            model.openTerminalAsync(entry.getRawFileEntry().getPath());
        }
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return new FontIcon("mdi2c-console");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().isDirectory());
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "Open in " + AppPrefs.get().terminalType().getValue().toTranslatedString();
    }
}
