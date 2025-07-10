package io.xpipe.app.browser.action.impl;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.browser.action.BrowserActionProvider;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.FileKind;
import io.xpipe.core.OsType;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class OpenFileNativeDetailsActionProvider implements BrowserActionProvider {

    @Jacksonized
    @SuperBuilder
    public static class Action extends BrowserAction {

        @Override
        public void executeImpl() throws Exception {
            ShellControl sc = model.getFileSystem().getShell().get();
            for (BrowserEntry entry : getEntries()) {
                var e = entry.getRawFileEntry().getPath();
                var localFile = sc.getLocalSystemAccess().translateToLocalSystemPath(e);
                switch (OsType.getLocal()) {
                    case OsType.Windows windows -> {
                        var parent = localFile.getParent();
                        // If we execute this on a drive root there will be no parent, so we have to check for that!
                        var content = parent != null
                                ? String.format(
                                        "$shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').ParseName('%s').InvokeVerb('Properties')",
                                        parent, localFile.getFileName())
                                : String.format(
                                        "$shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').Self.InvokeVerb('Properties')",
                                        localFile);

                        // The Windows shell invoke verb functionality behaves kinda weirdly and only shows the window
                        // as
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
                        var success = sc.executeSimpleBooleanCommand(dbus);
                        if (success) {
                            return;
                        }

                        sc.command(CommandBuilder.of()
                                        .add("xdg-open")
                                        .addFile(
                                                entry.getRawFileEntry().getKind() == FileKind.DIRECTORY
                                                        ? e
                                                        : e.getParent()))
                                .execute();
                    }
                    case OsType.MacOs macOs -> {
                        sc.osascriptCommand(String.format(
                                        """
                                 set fileEntry to (POSIX file "%s") as text
                                 tell application "Finder"
                                     activate
                                     open information window of alias fileEntry
                                 end tell
                                 """,
                                        localFile))
                                .execute();
                    }
                }
            }
        }
    }

    @Override
    public String getId() {
        return "openFileNativeDetails";
    }

    @Override
    public boolean isApplicable(BrowserFileSystemTabModel model, List<BrowserEntry> entries) {
        var sc = model.getFileSystem().getShell().orElseThrow();
        return sc.getLocalSystemAccess().supportsFileSystemAccess();
    }
}
