package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.util.FileOpener;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ToFileCommandAction implements LeafAction, ApplicationPathAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var command = createCommand(model, entry);
            try (var cc = sc.command(command).workingDirectory(model.getCurrentDirectory().getPath()).start()) {
                cc.discardErr();
                FileOpener.openCommandOutput(entry.getFileName(), entry, cc);
            }
        }
    }

    protected abstract String createCommand(OpenFileSystemModel model,  BrowserEntry entry);
}
