package io.xpipe.app.util;

import io.xpipe.app.ext.FileKind;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopHelper {

    private static final String[] browsers = {
        "xdg-open", "google-chrome", "firefox", "opera", "konqueror", "mozilla", "gnome-open", "open"
    };

    public static void openUrl(String uri) {
        if (uri == null) {
            return;
        }

        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return;
        }

        URI parsed;
        try {
            parsed = URI.create(uri);
        } catch (IllegalArgumentException e) {
            ErrorEventFactory.fromThrowable("Invalid URI: " + uri, e.getCause() != null ? e.getCause() : e)
                    .handle();
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            Desktop.getDesktop().browse(parsed);
        });
    }

    public static void browsePathRemote(ShellControl sc, FilePath path, FileKind kind) throws Exception {
        switch (sc.getOsType()) {
            case OsType.Windows ignored -> {
                // Explorer does not support single quotes, so use normal quotes
                if (kind == FileKind.DIRECTORY) {
                    sc.command(CommandBuilder.of().add("explorer").addQuoted(path.toString()))
                            .execute();
                } else {
                    sc.command(CommandBuilder.of().add("explorer", "/select,\"" + path.toString() + "\""))
                            .execute();
                }
            }
            case OsType.Linux ignored -> {
                var action = kind == FileKind.DIRECTORY
                        ? "org.freedesktop.FileManager1.ShowFolders"
                        : "org.freedesktop.FileManager1.ShowItems";
                var dbus = String.format(
                        """
                                         dbus-send --session --print-reply --dest=org.freedesktop.FileManager1 --type=method_call /org/freedesktop/FileManager1 %s array:string:"file://%s" string:""
                                         """,
                        action, path);
                var success = sc.executeSimpleBooleanCommand(dbus);
                if (success) {
                    return;
                }

                sc.command(CommandBuilder.of()
                                .add("xdg-open")
                                .addFile(kind == FileKind.DIRECTORY ? path : path.getParent()))
                        .execute();
            }
            case OsType.MacOs ignored -> {
                sc.command(CommandBuilder.of()
                                .add("open")
                                .addIf(kind == FileKind.DIRECTORY, "-R")
                                .addFile(path))
                        .execute();
            }
            default -> {}
        }
    }

    public static void browsePathLocal(Path file) {
        if (file == null) {
            return;
        }

        if (!Files.exists(file)) {
            return;
        }

        ThreadHelper.runAsync(() -> {
            var xdg = OsType.ofLocal() == OsType.LINUX;
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
                    && AppDistributionType.get() != AppDistributionType.WEBTOP) {
                try {
                    Desktop.getDesktop().browse(file.toFile().toURI());
                    return;
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).expected().omitted(xdg).handle();
                }
            }

            if (xdg) {
                LocalExec.readStdoutIfPossible("xdg-open", file.toString());
            }
        });
    }

    public static void browseFileInDirectory(Path file) {
        if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
            browsePathLocal(file.getParent());
            return;
        }

        ThreadHelper.runAsync(() -> {
            var xdg = OsType.ofLocal() == OsType.LINUX;
            if (AppDistributionType.get() != AppDistributionType.WEBTOP) {
                try {
                    Desktop.getDesktop().browseFileDirectory(file.toFile());
                    return;
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).expected().omitted(xdg).handle();
                }
            }

            if (xdg) {
                LocalExec.readStdoutIfPossible("xdg-open", file.getParent().toString());
            }
        });
    }
}
