package io.xpipe.app.browser.action;

import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ExecuteApplicationAction implements LeafAction, ApplicationPathAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var command = createCommand(model, entry);
            try (var cc = sc.command(command)
                    .withWorkingDirectory(model.getCurrentDirectory().getPath())
                    .start()) {
                cc.discardOrThrow();
            }
        }

        if (refresh()) {
            model.refreshSync();
        }
    }

    protected boolean refresh() {
        return false;
    }

    protected abstract String createCommand(OpenFileSystemModel model, BrowserEntry entry);
}
