package io.xpipe.app.browser.menu.impl.compress;

import io.xpipe.app.browser.menu.BrowserApplicationPathMenuProvider;
import io.xpipe.app.browser.menu.BrowserMenuLeafProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FilePath;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import java.util.List;

public class BaseUntarMenuProvider implements BrowserApplicationPathMenuProvider, BrowserMenuLeafProvider {

    private final boolean gz;
    private final boolean toDirectory;

    @Override
    public boolean isMutation() {
        return true;
    }

    public BaseUntarMenuProvider(boolean gz, boolean toDirectory) {
        this.gz = gz;
        this.toDirectory = toDirectory;
    }

    @Override
    public Node getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return BrowserIcons.createIcon(BrowserIconFileType.byId("zip")).createRegion();
    }

    @Override
    public String getExecutable() {
        return "tar";
    }

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        model.runAsync(
                () -> {
                    ShellControl sc = model.getFileSystem().getShell().orElseThrow();
                    for (BrowserEntry entry : entries) {
                        var target = getTarget(entry.getRawFileEntry().getPath());
                        var c = CommandBuilder.of().add("tar");
                        if (toDirectory) {
                            c.add("-C").addFile(target);
                        }
                        c.add("-x").addIf(gz, "-z").add("-f");
                        c.addFile(entry.getRawFileEntry().getPath());
                        if (toDirectory) {
                            model.getFileSystem().mkdirs(target);
                        }
                        sc.command(c)
                                .withWorkingDirectory(
                                        model.getCurrentDirectory().getPath())
                                .execute();
                    }
                },
                true);
    }

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var sep = model.getFileSystem().getShell().orElseThrow().getOsType().getFileSystemSeparator();
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
