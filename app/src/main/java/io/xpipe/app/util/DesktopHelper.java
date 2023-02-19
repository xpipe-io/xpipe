package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;

import java.awt.*;
import java.nio.file.Path;

public class DesktopHelper {

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
