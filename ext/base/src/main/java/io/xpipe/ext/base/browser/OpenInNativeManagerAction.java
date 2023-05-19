package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;

import java.util.List;

public class OpenInNativeManagerAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().get();
        ShellDialect d = sc.getShellDialect();
        for (FileBrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    sc.executeSimpleCommand("explorer " + d.fileArgument(e));
                }
                case OsType.Linux linux -> {
                    var dbus = String.format("""
                                                dbus-send --session --print-reply --dest=org.freedesktop.FileManager1 --type=method_call /org/freedesktop/FileManager1 org.freedesktop.FileManager1.ShowItems array:string:"%s" string:""
                                                """, entry.getRawFileEntry().getPath());
//                    sc.executeSimpleCommand(
//                            "xdg-open " + d.fileArgument(entry.getRawFileEntry().getPath()));
                    sc.executeSimpleCommand(dbus);
                }
                case OsType.MacOs macOs -> {
                    sc.executeSimpleCommand("open " + (entry.getRawFileEntry().isDirectory() ? "" : "-R ")
                            + d.fileArgument(entry.getRawFileEntry().getPath()));
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
    public boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return true;
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return switch (OsType.getLocal()) {
            case OsType.Windows windows -> "Browse in Windows Explorer";
            case OsType.Linux linux -> "Browse in default file manager";
            case OsType.MacOs macOs -> "Browse in Finder";
        };
    }
}
