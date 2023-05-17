package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.process.OsType;

import java.util.List;

public class OpenInNativeManagerAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        for (FileBrowserEntry entry : entries) {
            var e = entry.getRawFileEntry().getPath();
            switch (OsType.getLocal()) {
                case OsType.Windows windows -> {
                    model.getFileSystem()
                            .getShell()
                            .get()
                            .executeSimpleCommand("explorer "
                                    + model.getFileSystem()
                                            .getShell()
                                            .get()
                                            .getShellDialect()
                                            .fileArgument(e));
                }
                case OsType.Linux linux -> {}
                case OsType.MacOs macOs -> {}
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
        return entries.stream().allMatch(entry -> entry.getRawFileEntry().isDirectory());
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return switch (OsType.getLocal()) {
            case OsType.Windows windows -> "Open in Windows Explorer";
            case OsType.Linux linux -> "Open in Windows Explorer";
            case OsType.MacOs macOs -> "Open in Windows Explorer";
        };
    }
}
