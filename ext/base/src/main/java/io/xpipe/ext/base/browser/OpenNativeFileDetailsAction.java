package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public class OpenNativeFileDetailsAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().get();
        for (BrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    var parent = FileNames.getParent(e);
                    // If we execute this on a drive root there will be no parent, so we have to check for that!
                    var content = parent != null
                            ? String.format(
                                    "$shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').ParseName('%s').InvokeVerb('Properties')",
                                    FileNames.getParent(e), FileNames.getFileName(e))
                            : String.format(
                                    "$shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').Self.InvokeVerb('Properties')",
                                    e);

                    // The Windows shell invoke verb functionality behaves kinda weirdly and only shows the window as
                    // long as the parent process is running.
                    // So let's keep one process running
                    LocalStore.getLocalPowershell().command(content).notComplex().execute();
                }
                case OsType.Linux linux -> {
                    var dbus = String.format(
                            """
                                                dbus-send --session --print-reply --dest=org.freedesktop.FileManager1 --type=method_call /org/freedesktop/FileManager1 org.freedesktop.FileManager1.ShowItemProperties array:string:"file://%s" string:""
                                                """,
                            entry.getRawFileEntry().getPath());
                    sc.executeSimpleCommand(dbus);
                }
                case OsType.MacOs macOs -> {
                    sc.osascriptCommand(String.format(
                                    """
                             set fileEntry to (POSIX file "%s") as text
                             tell application "Finder" to open information window of file fileEntry
                             """,
                                    entry.getRawFileEntry().getPath()))
                            .execute();
                }
            }
        }
    }

    @Override
    public boolean acceptsEmptySelection() {
        return true;
    }

    @Override
    public Category getCategory() {
        return Category.NATIVE;
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell();
        return model.isLocal();
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Show details";
    }
}
