package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.action.impl.DeleteActionProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.store.FileKind;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class DeleteMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var link = entries.stream()
                .anyMatch(browserEntry ->
                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
        var files = entries.stream().map(browserEntry -> !link ?
                browserEntry.getRawFileEntry().resolved().getPath() : browserEntry.getRawFileEntry().getPath()).toList();
        var builder = DeleteActionProvider.Action.builder();
        builder.initFiles(model, files);
        return builder.build();
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2d-delete");
    }

    @Override
    public Category getCategory() {
        return Category.MUTATION;
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.DELETE);
    }

    @Override
    public boolean automaticallyResolveLinks() {
        return false;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable(
                "deleteFile",
                entries.stream()
                                .anyMatch(browserEntry ->
                                        browserEntry.getRawFileEntry().getKind() == FileKind.LINK)
                        ? "link"
                        : "");
    }
}
