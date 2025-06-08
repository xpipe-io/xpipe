package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class ComputeDirectorySizesAction implements BrowserMenuLeafProvider {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
        for (BrowserEntry be : model.getFileList().getAll().getValue()) {
            if (be.getRawFileEntry().getKind() != FileKind.DIRECTORY) {
                continue;
            }

            var size = model.getFileSystem().getDirectorySize(be.getRawFileEntry().getPath());
            var fileEntry = be.getRawFileEntry();
            fileEntry.setSize("" + size);
            model.getFileList().updateEntry(be, fileEntry);
        }
    }

    public String getId() {
        return "computeDirectorySizes";
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2f-format-list-text");
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("computeDirectorySizes");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.size() == 1 && entries.getFirst().getRawFileEntry().equals(model.getCurrentDirectory());
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }
}
