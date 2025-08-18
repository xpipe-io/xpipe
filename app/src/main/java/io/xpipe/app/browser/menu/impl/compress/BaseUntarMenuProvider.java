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
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.FilePath;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class BaseUntarMenuProvider implements BrowserApplicationPathMenuProvider, BrowserMenuLeafProvider {

    private final boolean gz;
    private final boolean toDirectory;

    public BaseUntarMenuProvider(boolean gz, boolean toDirectory) {
        this.gz = gz;
        this.toDirectory = toDirectory;
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.CompGraphic(BrowserIcons.createContextMenuIcon(BrowserIconFileType.byId("zip")));
    }

    @Override
    public String getExecutable() {
        return "tar";
    }

    @Override
    public AbstractAction createAction(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var builder = UntarActionProvider.Action.builder();
        builder.initEntries(model, entries);
        builder.gz(gz);
        builder.toDirectory(toDirectory);
        return builder.build();
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var sep = OsFileSystem.of(model.getFileSystem().getShell().orElseThrow().getOsType())
                .getFileSystemSeparator();
        var dir = entries.size() > 1
                ? "[...]"
                : getTarget(entries.getFirst().getRawFileEntry().getPath()).getFileName() + sep;
        return toDirectory ? AppI18n.observable("untarDirectory", dir) : AppI18n.observable("untarHere");
    }

    private FilePath getTarget(FilePath name) {
        return FilePath.of(name.toString()
                .replaceAll("\\.tar$", "")
                .replaceAll("\\.tar.gz$", "")
                .replaceAll("\\.tgz$", ""));
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        if (gz) {
            return entries.stream()
                    .allMatch(entry -> entry.getRawFileEntry()
                                    .getPath()
                                    .toString()
                                    .endsWith(".tar.gz")
                            || entry.getRawFileEntry().getPath().toString().endsWith(".tgz"));
        }

        return entries.stream()
                .allMatch(entry -> entry.getRawFileEntry().getPath().toString().endsWith(".tar"));
    }
}
