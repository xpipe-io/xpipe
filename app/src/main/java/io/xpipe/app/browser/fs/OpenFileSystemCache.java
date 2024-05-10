package io.xpipe.app.browser.fs;

import io.xpipe.app.util.ShellControlCache;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;

import lombok.Getter;

@Getter
public class OpenFileSystemCache extends ShellControlCache {

    private final OpenFileSystemModel model;
    private final String username;

    public OpenFileSystemCache(OpenFileSystemModel model) throws Exception {
        super(model.getFileSystem().getShell().orElseThrow());
        this.model = model;

        ShellControl sc = model.getFileSystem().getShell().get();
        ShellDialect d = sc.getShellDialect();
        // If there is no id command, we should still be fine with just assuming root
        username = d.printUsernameCommand(sc).readStdoutIfPossible().orElse("root");
    }

    public boolean isRoot() {
        return username.equals("root");
    }
}
