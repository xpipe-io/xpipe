package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.session.BrowserSessionModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenDirectoryInNewTabAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
        if (model.getBrowserModel() instanceof BrowserSessionModel bm) {
            bm.openFileSystemAsync(
                    model.getEntry(), m -> entries.getFirst().getRawFileEntry().getPath(), null);
        }
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
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("openInNewTab");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getBrowserModel() instanceof BrowserSessionModel
                && entries.size() == 1
                && entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }
}
