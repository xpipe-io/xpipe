package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserClipboard;
import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class PasteAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        var clipboard = FileBrowserClipboard.retrieveCopy();
        if (clipboard == null) {
            return;
        }

        var target = entries.size() == 1 && entries.get(0).getRawFileEntry().isDirectory() ? entries.get(0).getRawFileEntry() : model.getCurrentDirectory();
        var files = clipboard.getEntries();
        model.dropFilesIntoAsync(target, files, true);
    }

    @Override
    public Category getCategory() {
        return Category.COPY_PASTE;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return new FontIcon("mdi2c-content-paste");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return entries.size() < 2 && entries.stream().allMatch(entry -> entry.getRawFileEntry().isDirectory());
    }

    @Override
    public boolean isActive(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return FileBrowserClipboard.retrieveCopy() != null;
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
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "Paste";
    }
}
