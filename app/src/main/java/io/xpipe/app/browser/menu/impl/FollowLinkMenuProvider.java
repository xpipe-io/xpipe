package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class FollowLinkMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var target = entries.getFirst().getRawFileEntry().resolved().getPath().getParent();
        model.cdAsync(target);
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2a-arrow-top-right-thick");
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("followLink");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
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
