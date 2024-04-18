package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileNames;

import javafx.beans.value.ObservableValue;

import java.util.List;

public class OpenNativeFileDetailsAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().get();
        for (BrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            var localFile = sc.getLocalSystemAccess().translateToLocalSystemPath(e);
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    var parent = FileNames.getParent(localFile);
                    // If we execute this on a drive root there will be no parent, so we have to check for that!
                    var content = parent != null
                            ? String.format(
                                    "$shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').ParseName('%s').InvokeVerb('Properties')",
                                    FileNames.getParent(localFile), FileNames.getFileName(localFile))
                            : String.format(
                                    "$shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').Self.InvokeVerb('Properties')",
                                    localFile);

                    // The Windows shell invoke verb functionality behaves kinda weirdly and only shows the window as
                    // long as the parent process is running.
                    // So let's keep one process running
                    LocalShell.getLocalPowershell()
                            .command(content)
                            .notComplex()
                            .execute();
                }
                case OsType.Linux linux -> {
                    var dbus = String.format(
                            """
                                                dbus-send --session --print-reply --dest=org.freedesktop.FileManager1 --type=method_call /org/freedesktop/FileManager1 org.freedesktop.FileManager1.ShowItemProperties array:string:"file://%s" string:""
                                                """,
                            localFile);
                    sc.executeSimpleCommand(dbus);
                }
                case OsType.MacOs macOs -> {
                    sc.osascriptCommand(String.format(
                                    """
                             set fileEntry to (POSIX file "%s") as text
                             tell application "Finder" to open information window of alias fileEntry
                             """,
                                    localFile))
                            .execute();
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
        return AppI18n.observable("showDetails");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        return sc.getLocalSystemAccess().supportsFileSystemAccess();
    }
}
