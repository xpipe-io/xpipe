package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class FollowLinkAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var target = FileNames.getParent(
                entries.getFirst().getRawFileEntry().resolved().getPath());
        model.cdAsync(target);
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-arrow-top-right-thick");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("followLink");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.size() == 1
                && entries.stream()
                        .allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.LINK
                                && entry.getRawFileEntry().resolved().getKind() != FileKind.DIRECTORY);
    }

    @Override
    public boolean automaticallyResolveLinks() {
        return false;
    }
}
