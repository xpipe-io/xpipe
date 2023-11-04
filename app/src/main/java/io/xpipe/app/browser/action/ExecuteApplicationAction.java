package io.xpipe.app.browser.action;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ExecuteApplicationAction implements LeafAction, ApplicationPathAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (BrowserEntry entry : entries) {
            var command = detach() ? ScriptHelper.createDetachCommand(sc, createCommand(model, entry)) : createCommand(model, entry);
            try (var cc = sc.command(command).withWorkingDirectory(model.getCurrentDirectory().getPath()).start()) {
                cc.discardOrThrow();
            }
        }

        if (detach() && refresh()) {
            throw new IllegalStateException();
        }

        if (refresh()) {
            model.refreshSync();
        }
    }

    protected boolean detach() {
        return false;
    }

    protected boolean refresh() {
        return false;
    }

    protected abstract String createCommand(OpenFileSystemModel model, BrowserEntry entry);
}
