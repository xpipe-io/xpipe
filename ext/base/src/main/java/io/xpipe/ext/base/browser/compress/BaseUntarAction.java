package io.xpipe.ext.base.browser.compress;

import io.xpipe.app.browser.action.ApplicationPathAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.browser.icon.BrowserIconFileType;
import io.xpipe.app.browser.icon.BrowserIcons;
import io.xpipe.app.core.AppI18n;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;

import java.util.List;

public class BaseUntarAction implements ApplicationPathAction, LeafAction {

    private final boolean gz;
    private final boolean toDirectory;

    public BaseUntarAction(boolean gz, boolean toDirectory) {
        this.gz = gz;
        this.toDirectory = toDirectory;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return BrowserIcons.createIcon(BrowserIconFileType.byId("zip")).createRegion();
    }

    @Override
    public String getExecutable() {
        return "tar";
    }

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        model.runAsync(() -> {
            ShellControl sc = model.getFileSystem().getShell().orElseThrow();
            for (BrowserEntry entry : entries) {
                var target = getTarget(entry.getRawFileEntry().getPath());
                var c = CommandBuilder.of().add("tar");
                if (toDirectory) {
                    c.add("-C").addFile(target);
                }
                c.add("-xv").addIf(gz, "-z").add("-f");
                c.addFile(entry.getRawFileEntry().getPath());
                if (toDirectory) {
                    model.getFileSystem().mkdirs(target);
                }
                sc.command(c).withWorkingDirectory(model.getCurrentDirectory().getPath()).execute();
            }
        }, true);
    }

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sep = model.getFileSystem().getShell().orElseThrow().getOsType().getFileSystemSeparator();
        var dir = entries.size() > 1 ? "[...]" : getTarget(entries.getFirst().getFileName()) + sep;
        return toDirectory ? AppI18n.observable("untarDirectory", dir) : AppI18n.observable("untarHere");
    }

    private String getTarget(String name) {
        return name.replaceAll("\\.tar$", "").replaceAll("\\.tar.gz$", "").replaceAll("\\.tgz$", "");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        if (gz) {
            return entries.stream().allMatch(entry -> entry.getRawFileEntry().getPath().endsWith(".tar.gz") || entry.getRawFileEntry().getPath().endsWith(".tgz"));
        }

        return entries.stream().allMatch(entry -> entry.getRawFileEntry().getPath().endsWith(".tar"));
    }
}
