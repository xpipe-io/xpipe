package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;

import java.awt.*;
import java.nio.file.Path;

public class DesktopHelper {

    public static Path getDesktopDirectory() throws Exception {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return Path.of(LocalStore.getLocalPowershell().executeSimpleStringCommand("[Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)"));
        }

        return Path.of(System.getProperty("user.home") + "/Desktop");
    }

    public static void browsePath(Path file) {
        if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return;
        }

        ThreadHelper.runAsync(() -> {
            try {
                Desktop.getDesktop().open(file.toFile());
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }

    public static void browseFileInDirectory(Path file) {
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                return;
            }

            ThreadHelper.runAsync(() -> {
                try {
                    Desktop.getDesktop().open(file.getParent().toFile());
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).omit().handle();
                }
            });
            return;
        }

        ThreadHelper.runAsync(() -> {
            try {
                Desktop.getDesktop().browseFileDirectory(file.toFile());
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        });
    }
}
