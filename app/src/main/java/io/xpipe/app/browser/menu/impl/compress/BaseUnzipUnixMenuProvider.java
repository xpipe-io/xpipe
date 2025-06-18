package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.browser.menu.BrowserApplicationPathMenuProvider;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.process.OsType;

import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class BaseUnzipUnixMenuProvider implements BrowserMenuLeafProvider, BrowserApplicationPathMenuProvider {

    private final boolean toDirectory;

    public BaseUnzipUnixMenuProvider(boolean toDirectory) {
        this.toDirectory = toDirectory;
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.CompGraphic(BrowserIcons.createContextMenuIcon(BrowserIconFileType.byId("zip")));
    }

    @Override
    public String getExecutable() {
        return "unzip";
    }

    @Override
    public AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var builder = UnzipActionProvider.Action.builder();
        builder.initEntries(model, entries);
        builder.toDirectory(toDirectory);
        return builder.build();
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var sep = model.getFileSystem().getShell().orElseThrow().getOsType().getFileSystemSeparator();
        var dir = entries.size() > 1
                ? "[...]"
                : UnzipActionProvider.getTarget(
                                        entries.getFirst().getRawFileEntry().getPath())
                                .getFileName()
                        + sep;
        return toDirectory ? AppI18n.observable("unzipDirectory", dir) : AppI18n.observable("unzipHere");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream()
                        .allMatch(entry ->
                                entry.getRawFileEntry().getPath().toString().endsWith(".zip"))
                && !model.getFileSystem().getShell().orElseThrow().getOsType().equals(OsType.WINDOWS);
    }
}
