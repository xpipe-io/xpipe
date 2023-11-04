package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.store.FileKind;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenDirectoryInNewTabAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
        model.getBrowserModel().openFileSystemAsync(model.getEntry(), entries.get(0).getRawFileEntry().getPath(), null);
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2f-folder-open-outline");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Open in new tab";
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.size() == 1 && entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }
}
