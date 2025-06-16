package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FilePath;

import javafx.beans.value.ObservableValue;

import java.util.List;

public abstract class BaseUnzipWindowsActionProvider implements BrowserMenuLeafProvider {

    private final boolean toDirectory;

    public BaseUnzipWindowsActionProvider(boolean toDirectory) {
        this.toDirectory = toDirectory;
    }

    @Override
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return new LabelGraphic.CompGraphic(BrowserIcons.createContextMenuIcon(BrowserIconFileType.byId("zip")));
    }

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        model.runAsync(
                () -> {
                    var sc = model.getFileSystem().getShell().orElseThrow();
                    if (ShellDialects.isPowershell(sc)) {
                        for (BrowserEntry entry : entries) {
                            runCommand(sc, model, entry);
                        }
                    } else {
                        try (var sub = sc.subShell(ShellDialects.POWERSHELL)) {
                            for (BrowserEntry entry : entries) {
                                runCommand(sub, model, entry);
                            }
                        }
                    }
                },
                true);
    }

    private void runCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry) throws Exception {
        var command = CommandBuilder.of().add("Expand-Archive", "-Force");
        if (toDirectory) {
            var target = getTarget(entry.getRawFileEntry().getPath());
            command.add("-DestinationPath").addFile(target);
        }
        command.add("-Path").addFile(entry.getRawFileEntry().getPath());
        sc.command(command)
                .withWorkingDirectory(model.getCurrentDirectory().getPath())
                .execute();
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
                : getTarget(entries.getFirst().getRawFileEntry().getPath()).getFileName() + sep;
        return toDirectory ? AppI18n.observable("unzipDirectory", dir) : AppI18n.observable("unzipHere");
    }

    private FilePath getTarget(FilePath name) {
        return FilePath.of(name.toString().replaceAll("\\.zip$", ""));
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream()
                        .allMatch(entry ->
                                entry.getRawFileEntry().getPath().toString().endsWith(".zip"))
                && model.getFileSystem().getShell().orElseThrow().getOsType().equals(OsType.WINDOWS);
    }
}
