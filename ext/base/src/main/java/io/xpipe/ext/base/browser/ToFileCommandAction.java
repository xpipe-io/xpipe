package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.action.ApplicationPathAction;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.app.browser.file.BrowserEntry;
import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ToFileCommandAction implements LeafAction, ApplicationPathAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var command = createCommand(model, entry);
            var out = sc.command(command)
                    .withWorkingDirectory(model.getCurrentDirectory().getPath())
                    .readStdoutOrThrow();
            FileOpener.openReadOnlyString(out);
        }
    }

    protected abstract String createCommand(OpenFileSystemModel model, BrowserEntry entry);
}
