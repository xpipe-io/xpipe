package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.action.impl.ComputeDirectorySizesActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class ComputeDirectorySizesMenuProvider implements BrowserMenuLeafProvider {

    @Override
    public AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var builder = ComputeDirectorySizesActionProvider.Action.builder();
        builder.initEntries(model, entries);
        return builder.build();
    }

    public String getId() {
        return "computeDirectorySizes";
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.IconGraphic("mdi2f-format-list-text");
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var topLevel =
                entries.size() == 1 && entries.getFirst().getRawFileEntry().equals(model.getCurrentDirectory());
        return AppI18n.observable(topLevel ? "computeDirectorySizes" : "computeSize");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream()
                .allMatch(browserEntry -> browserEntry.getRawFileEntry().getKind() == FileKind.DIRECTORY);
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.ACTION;
    }
}
