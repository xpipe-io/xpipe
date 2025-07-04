package io.xpipe.app.browser.menu.impl;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.browser.menu.BrowserMenuCategory;
import io.xpipe.app.browser.menu.MultiExecuteMenuProvider;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LabelGraphic;
import io.xpipe.core.FileKind;
import io.xpipe.core.OsType;

import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.stream.Stream;

public class RunFileMenuProvider extends MultiExecuteMenuProvider {

    private boolean isExecutable(FileEntry e) {
        if (e.getKind() != FileKind.FILE) {
            return false;
        }

        if (e.getInfo() != null && e.getInfo().possiblyExecutable()) {
            return true;
        }

        var shell = e.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return false;
        }

        var os = shell.get().getOsType();
        if (os.equals(OsType.WINDOWS)
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
    public LabelGraphic getIcon(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
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

    protected CommandBuilder createCommand(ShellControl sc, BrowserFileSystemTabModel model, BrowserEntry entry) {
        return CommandBuilder.of()
                .add(sc.getShellDialect()
                        .runScriptCommand(sc, entry.getRawFileEntry().getPath().toString()));
    }
}
