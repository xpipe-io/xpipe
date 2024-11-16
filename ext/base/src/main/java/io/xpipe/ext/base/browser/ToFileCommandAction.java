package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.BrowserApplicationPathAction;
import io.xpipe.app.browser.action.BrowserLeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.file.BrowserFileSystemTabModel;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ToFileCommandAction implements BrowserLeafAction, BrowserApplicationPathAction {

    @Override
    public void execute(BrowserFileSystemTabModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var command = createCommand(model, entry);
            var out = sc.command(command)
                    .withWorkingDirectory(model.getCurrentDirectory().getPath())
                    .readStdoutOrThrow();
            FileOpener.openReadOnlyString(out);
        }
    }

    protected abstract String createCommand(BrowserFileSystemTabModel model, BrowserEntry entry);
}
