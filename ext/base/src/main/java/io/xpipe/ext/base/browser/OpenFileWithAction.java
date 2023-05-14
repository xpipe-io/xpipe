package io.xpipe.ext.base.browser;

import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinUser;
import io.xpipe.app.browser.FileBrowserEntry;
import io.xpipe.app.browser.OpenFileSystemModel;
import io.xpipe.app.browser.action.LeafAction;
import io.xpipe.core.process.OsType;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

public class OpenFileWithAction implements LeafAction {

    @Override
    public void execute(OpenFileSystemModel model, List<FileBrowserEntry> entries) throws Exception {
        switch (OsType.getLocal()) {
            case OsType.Windows windows -> {
                Shell32.INSTANCE.ShellExecute(
                        null,
                        "open",
                        "rundll32.exe",
                        "shell32.dll,OpenAs_RunDLL " + entries.get(0).getRawFileEntry().getPath(),
                        null,
                        WinUser.SW_SHOWNORMAL);
            }
            case OsType.Linux linux -> {
            }
            case OsType.MacOs macOs -> {
            }
        }
    }

    @Override
    public Category getCategory() {
        return Category.OPEN;
    }

    @Override
    public Node getIcon(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return new FontIcon("mdi2b-book-open-page-variant-outline");
    }

    @Override
    public boolean isApplicable(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return entries.size() == 1 && entries.stream().noneMatch(entry -> entry.getRawFileEntry().isDirectory());
    }

    @Override
    public KeyCombination getShortcut() {
        return new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHIFT_DOWN);
    }

    @Override
    public String getName(OpenFileSystemModel model, List<FileBrowserEntry> entries) {
        return "Open with ...";
    }
}
