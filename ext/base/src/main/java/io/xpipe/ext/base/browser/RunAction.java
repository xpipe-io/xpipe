package io.xpipe.ext.base.browser;

import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.MultiExecuteAction;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileSystem;
import javafx.scene.Node;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class RunAction extends MultiExecuteAction {

    private boolean isExecutable(FileSystem.FileEntry e) {
        if (e.isDirectory()) {
            return false;
        }

        if (e.getExecutable() != null && e.getExecutable()) {
            return true;
        }

        var shell = e.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return false;
        }

        var os = shell.get().getOsType();
        if (os.equals(OsType.WINDOWS) && List.of("exe", "bat", "ps1", "cmd").stream().anyMatch(s -> e.getPath().endsWith(s))) {
            return true;
        }

        if (List.of("sh", "command").stream().anyMatch(s -> e.getPath().endsWith(s))) {
            return true;
        }

        return false;
    }

    @Override
    public Category getCategory() {
        return Category.CUSTOM;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2p-play");
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Run";
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return entries.stream().allMatch(entry -> isExecutable(entry.getRawFileEntry()));
    }

    @Override
    protected String createCommand(ShellControl sc, OpenFileSystemModel model, BrowserEntry entry) {
        return sc.getShellDialect().runScript(entry.getFileName());
    }
}
