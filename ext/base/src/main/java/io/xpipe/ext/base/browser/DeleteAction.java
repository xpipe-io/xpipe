package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserAlerts;
import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.FileSystemHelper;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DeleteAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        var toDelete = entries.stream().map(entry -> entry.getRawFileEntry()).toList();
        if (!FileBrowserAlerts.showDeleteAlert(toDelete)) {
            return;
        }

        FileSystemHelper.delete(toDelete);
        model.refreshSync();
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return new FontIcon("mdi2d-delete");
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.DELETE);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "Delete";
    }
}
