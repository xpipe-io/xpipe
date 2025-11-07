package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.MultiExecuteMenuProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileKind;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.core.OsType;

import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.stream.Stream;

public class RunFileMenuProvider extends MultiExecuteMenuProvider {

    private boolean isExecutable(FileEntry e) {
        if (e.getKind() != FileKind.FILE) {
            return false;
        }

        var shell = e.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return false;
        }
        var os = shell.get().getOsType();

        if (e.getInfo() != null && e.getInfo().possiblyExecutable() && os != OsType.WINDOWS) {
            return true;
        }

        if (os == OsType.WINDOWS
                && Stream.of("exe", "bat", "ps1", "cmd")
                        .anyMatch(s -> e.getPath().toString().endsWith(s))) {
            return true;
        }

        if (ShellDialects.isPowershell(shell.get())
                && Stream.of("ps1").anyMatch(s -> e.getPath().toString().endsWith(s))) {
            return true;
        }

        if (Stream.of("sh", "command").anyMatch(s -> e.getPath().toString().endsWith(s))) {
            return true;
        }

        return false;
    }

    @Override
    public LabelGraphic getIcon() {
        return new LabelGraphic.IconGraphic("mdi2p-play");
    }

    @Override
    public BrowserMenuCategory getCategory() {
        return BrowserMenuCategory.CUSTOM;
    }

    @Override
    public ObservableValue<String> getName(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return AppI18n.observable("run");
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> isExecutable(entry.getRawFileEntry()));
    }

    @Override
    protected List<CommandBuilder> createCommand(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        return entries.stream()
                .map(browserEntry -> {
                    return CommandBuilder.of()
                            .add(sc.getShellDialect()
                                    .runScriptCommand(
                                            sc,
                                            browserEntry
                                                    .getRawFileEntry()
                                                    .getPath()
                                                    .toString()));
                })
                .toList();
    }
}
