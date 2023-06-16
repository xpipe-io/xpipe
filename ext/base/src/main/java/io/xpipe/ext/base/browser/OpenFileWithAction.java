package io.xpipe.ext.base.browser;

import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinUser;
import io.xpipe.app.browser.BrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialect;
import io.xpipe.core.store.FileKind;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenFileWithAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<BrowserEntry> entries) throws Exception {
        switch (OsType.getLocal()) {
            case OsType.Windows windows -> {
                Shell32.INSTANCE.ShellExecute(
                        null,
                        "open",
                        "rundll32.exe",
                        "shell32.dll,OpenAs_RunDLL "
                                + entries.get(0).getRawFileEntry().getPath(),
                        null,
                        WinUser.SW_SHOWNORMAL);
            }
            case OsType.Linux linux -> {
                ShellControl sc = model.getFileSystem().getShell().get();
                ShellDialect d = sc.getShellDialect();
                sc.executeSimpleCommand("mimeopen -a "
                        + d.fileArgument(entries.get(0).getRawFileEntry().getPath()));
            }
            case OsType.MacOs macOs -> {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return new FontIcon("mdi2b-book-open-page-variant-outline");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<BrowserEntry> entries) {
        var os = model.getFileSystem().getShell();
        return os.isPresent()
                && os.get().getOsType().equals(OsType.WINDOWS)
                && entries.size() == 1
                && entries.stream().allMatch(entry -> entry.getRawFileEntry().getKind() == FileKind.FILE);
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<BrowserEntry> entries) {
        return "Open with ...";
    }
}
