package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.FileSystemHelper;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DeleteLinkAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        var toDelete = entries.stream().map(entry -> entry.getRawFileEntry()).toList();
        FileSystemHelper.delete(toDelete);
        model.refreshSync();
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2d-delete");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("deleteLink");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.stream()
                .allMatch(browserEntry -> browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
    }

    @Override
    public boolean automaticallyResolveLinks() {
        return false;
    }
}
