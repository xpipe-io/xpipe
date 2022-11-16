package io.xpipe.extension.util;

import io.xpipe.extension.event.ErrorEvent;
import org.apache.commons.lang3.SystemUtils;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OsHelper {

    public static String getFileSystemCompatibleName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static Path getUserDocumentsPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return Paths.get(System.getProperty("user.home"));
        } else {
            return Paths.get(System.getProperty("user.home"), ".local", "share");
        }
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
