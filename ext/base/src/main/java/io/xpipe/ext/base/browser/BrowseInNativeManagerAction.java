package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.FileKind;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class BrowseInNativeManagerAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        ShellDialect d = sc.getShellDialect();
        for (BrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            var localFile = sc.getLocalSystemAccess().translateToLocalSystemPath(e);
            try (var local = LocalShell.getShell().start()) {
                switch (OsType.getLocal()) {
                    case OsType.Windows windows -> {
                        // Explorer does not support single quotes, so use normal quotes
                        if (entry.getRawFileEntry().getKind() == FileKind.DIRECTORY) {
                            local.executeSimpleCommand("explorer " + d.quoteArgument(localFile));
                        } else {
                            local.executeSimpleCommand("explorer /select," + d.quoteArgument(localFile));
                        }
                    }
                    case OsType.Linux linux -> {
                        var action = entry.getRawFileEntry().getKind() == FileKind.DIRECTORY ?
                                "org.freedesktop.FileManager1.ShowFolders" :
                                "org.freedesktop.FileManager1.ShowItems";
                        var dbus = String.format("""
                                                 dbus-send --session --print-reply --dest=org.freedesktop.FileManager1 --type=method_call /org/freedesktop/FileManager1 %s array:string:"file://%s" string:""
                                                 """, action, localFile);
                        local.executeSimpleCommand(dbus);
                    }
                    case OsType.MacOs macOs -> {
                        local.executeSimpleCommand(
                                "open " + (entry.getRawFileEntry().getKind() == FileKind.DIRECTORY ? "" : "-R ") + d.fileArgument(localFile));
                    }
                }
            }
        }
    }

    @Override
    public Category getCategory() {
        return Category.NATIVE;
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public ObservableValue<String> getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return switch (OsType.getLocal()) {
            case OsType.Windows windows -> AppI18n.observable("browseInWindowsExplorer");
            case OsType.Linux linux -> AppI18n.observable("browseInDefaultFileManager");
            case OsType.MacOs macOs -> AppI18n.observable("browseInFinder");
        };
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return model.getFileSystem()
                .getShell()
                .orElseThrow()
                .getLocalSystemAccess()
                .supportsFileSystemAccess();
    }
}
