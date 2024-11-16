package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserApplicationPathAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ExecuteApplicationAction implements BrowserLeafAction, BrowserApplicationPathAction {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
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

    protected abstract CommandBuilder createCommand(BrowserFileSystemTabModel model, BrowserEntry entry);
}
