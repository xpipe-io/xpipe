package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;

import java.util.List;

public class OpenNativeFileDetailsAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().get();
        for (FileBrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    var content = String.format(
                            """
                                    $shell = New-Object -ComObject Shell.Application; $shell.NameSpace('%s').ParseName('%s').InvokeVerb('Properties')
                                    """,
                            FileNames.getParent(e), FileNames.getFileName(e));
                    try (var sub = sc.enforcedDialect(ShellDialects.POWERSHELL).start()) {
                        sub.command(content).notComplex().execute();
                    }
                }
                case OsType.Linux linux -> {
                    throw new UnsupportedOperationException();
                }
                case OsType.MacOs macOs -> {
                    sc.osascriptCommand(String.format(
                            """
                             set fileEntry to (POSIX file "%s") as text
                             tell application "Finder" to open information window of file fileEntry
                             """,
                            entry.getRawFileEntry().getPath())).execute();
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
    public boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        var os = model.getFileSystem().getShell();
        return os.isPresent() && !os.get().getOsType().equals(OsType.LINUX);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "Show details";
    }
}
