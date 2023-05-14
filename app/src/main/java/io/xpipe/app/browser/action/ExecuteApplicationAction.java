package io.xpipe.app.browser.action;

import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.ShellControl;

import java.util.List;

public abstract class ExecuteApplicationAction implements LeafAction, ApplicationPathAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        ShellControl sc = model.getFileSystem().getShell().orElseThrow();
        for (FileBrowserEntry entry : entries) {
            var command = detach() ? ScriptHelper.createDetachCommand(sc, createCommand(model, entry)) : createCommand(model, entry);
            try (var cc = sc.command(command).workingDirectory(model.getCurrentDirectory().getPath()).start()) {
                cc.discardOrThrow();
            }
        }
    }

    protected boolean detach() {
        return false;
    }

    protected abstract String createCommand(OpenFileSystemModel model,  FileBrowserEntry entry);

}
