package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileKind;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopHelper {

    public static Path getDesktopDirectory() throws Exception {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return Path.of(LocalShell.getLocalPowershell()
                    .executeSimpleStringCommand("[Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)"));
        } else if (OsType.getLocal() == OsType.LINUX) {
            try (var cmd = LocalShell.getShell().command("xdg-user-dir DESKTOP").start()) {
                var read = cmd.readStdoutDiscardErr();
                var exit = cmd.getExitCode();
                if (exit == 0) {
                    return Path.of(read);
                }
            }
        }

        return Path.of(System.getProperty("user.home") + "/Desktop");
    }

    public static void browsePathRemote(ShellControl sc, String path, FileKind kind) throws Exception {
        var d = sc.getShellDialect();
        switch (sc.getOsType()) {
            case OsType.Windows windows -> {
                // Explorer does not support single quotes, so use normal quotes
                if (kind == FileKind.DIRECTORY) {
                    sc.executeSimpleCommand("explorer " + d.quoteArgument(path));
                } else {
                    sc.executeSimpleCommand("explorer /select," + d.quoteArgument(path));
                }
            }
            case OsType.Linux linux -> {
                var action = kind == FileKind.DIRECTORY
                        ? "org.freedesktop.FileManager1.ShowFolders"
                        : "org.freedesktop.FileManager1.ShowItems";
                var dbus = String.format(
                        """
                                                 dbus-send --session --print-reply --dest=org.freedesktop.FileManager1 --type=method_call /org/freedesktop/FileManager1 %s array:string:"file://%s" string:""
                                                 """,
                        action, path);
                sc.executeSimpleCommand(dbus);
            }
            case OsType.MacOs macOs -> {
                sc.executeSimpleCommand("open " + (kind == FileKind.DIRECTORY ? "" : "-R ") + d.fileArgument(path));
            }
            case OsType.Bsd bsd -> {}
            case OsType.Solaris solaris -> {}
        }
    }

    public static void browsePathLocal(Path file) {
        if (!Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return;
        }

        if (!Files.exists(file)) {
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
