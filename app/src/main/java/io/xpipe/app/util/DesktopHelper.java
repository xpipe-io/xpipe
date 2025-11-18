package io.xpipe.app.util;

import io.xpipe.app.ext.FileKind;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DesktopHelper {

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

        // This can be a blocking operation
        ThreadHelper.runAsync(() -> {
            try {
                Desktop.getDesktop().browse(parsed);
                return;
            } catch (Exception e) {
                // Some basic linux systems have trouble with the API call
                ErrorEventFactory.fromThrowable(e)
                        .expected()
                        .omitted(OsType.ofLocal() == OsType.LINUX)
                        .handle();
            }

            if (OsType.ofLocal() == OsType.LINUX) {
                LocalExec.readStdoutIfPossible("xdg-open", parsed.toString());
            }
        });
    }

    public static void browseFile(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }

        // This can be a blocking operation
        ThreadHelper.runAsync(() -> {
            if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(file.toFile().toURI());
                    return;
                } catch (Exception e) {
                    // Some basic linux systems have trouble with the API call
                    ErrorEventFactory.fromThrowable(e)
                            .expected()
                            .omitted(OsType.ofLocal() == OsType.LINUX)
                            .handle();
                }
            }

            if (OsType.ofLocal() == OsType.LINUX) {
                LocalExec.readStdoutIfPossible("xdg-open", file.toString());
            }
        });
    }

    public static void browseFileInDirectory(Path file) {
        if (file == null || !Files.exists(file)) {
            return;
        }

        // This can be a blocking operation
        ThreadHelper.runAsync(() -> {
            // Windows does not support Action.BROWSE_FILE_DIR
            if (OsType.ofLocal() == OsType.WINDOWS) {
                // Explorer does not support single quotes, so use normal quotes
                if (Files.isDirectory(file)) {
                    LocalExec.readStdoutIfPossible("explorer", "\"" + file + "\"");
                } else {
                    LocalExec.readStdoutIfPossible("explorer", "/select,", "\"" + file + "\"");
                }
                return;
            }

            // Linux does not support Action.BROWSE_FILE_DIR
            if (OsType.ofLocal() == OsType.LINUX) {
                var action = Files.isDirectory(file)
                        ? "org.freedesktop.FileManager1.ShowFolders"
                        : "org.freedesktop.FileManager1.ShowItems";
                var args = List.of(
                        "dbus-send",
                        "--session",
                        "--print-reply",
                        "--dest=org.freedesktop.FileManager1",
                        "--type=method_call",
                        "/org/freedesktop/FileManager1",
                        action,
                        "array:string:file://" + file,
                        "string:");
                try {
                    var success = LocalExec.readStdoutIfPossible(args.toArray(String[]::new))
                            .isPresent();
                    if (success) {
                        return;
                    }
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).omit().handle();
                }
            }

            if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                browseFile(file.getParent());
                return;
            }

            try {
                Desktop.getDesktop().browseFileDirectory(file.toFile());
            } catch (Exception e) {
                // Some basic linux systems have trouble with the API call
                ErrorEventFactory.fromThrowable(e)
                        .expected()
                        .omitted(OsType.ofLocal() == OsType.LINUX)
                        .handle();
                if (OsType.ofLocal() == OsType.LINUX) {
                    browseFile(file.getParent());
                }
            }
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
                    sc.command(CommandBuilder.of().add("explorer", "/select,", "\"" + path.toString() + "\""))
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
}
