package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.action.impl.DeleteActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;

public class DeleteMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var link = entries.stream()
                .anyMatch(browserEntry -> browserEntry.getRawFileEntry().getKind() == FileKind.LINK);
        var files = entries.stream()
                .map(browserEntry -> !link
                        ? browserEntry.getRawFileEntry().resolved().getPath()
                        : browserEntry.getRawFileEntry().getPath())
                .toList();
        var builder = DeleteActionProvider.Action.builder();
        builder.initFiles(model, files);
        return builder.build();
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2d-delete");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.MUTATION;
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
