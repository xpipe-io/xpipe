package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.DesktopHelper;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import javafx.beans.value.ObservableValue;

import java.util.List;

public class BrowseInNativeManagerAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            var localFile = sc.getLocalSystemAccess().translateToLocalSystemPath(e);
            try (var local = LocalShell.getShell().start()) {
                DesktopHelper.browsePathRemote(local,localFile, entry.getRawFileEntry().getKind());
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
