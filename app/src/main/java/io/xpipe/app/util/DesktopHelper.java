package io.xpipe.app.util;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class DesktopHelper {

    private static final String[] browsers = {
        "xdg-open", "google-chrome", "firefox", "opera", "konqueror", "mozilla", "gnome-open", "open"
    };

    public static void openUrl(String uri) {
        try {
            if (OsType.getLocal() == OsType.WINDOWS) {
                var pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri);
                pb.directory(AppSystemInfo.ofCurrent().getUserHome().toFile());
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start();
            } else if (OsType.getLocal() == OsType.LINUX) {
                String browser = null;
                for (String b : browsers) {
                    if (browser == null
                            && Runtime.getRuntime()
                                            .exec(new String[] {"which", b})
                                            .getInputStream()
                                            .read()
                                    != -1) {
                        Runtime.getRuntime().exec(new String[] {browser = b, uri});
                    }
                }
            } else {
                var pb = new ProcessBuilder("open", uri);
                pb.directory(AppSystemInfo.ofCurrent().getUserHome().toFile());
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start();
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
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
            case OsType.Bsd ignored -> {}
            case OsType.Solaris ignored -> {}
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
            var xdg = OsType.getLocal() == OsType.LINUX;
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
                    && AppDistributionType.get() != AppDistributionType.WEBTOP) {
                try {
                    Desktop.getDesktop().open(file.toFile());
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
            var xdg = OsType.getLocal() == OsType.LINUX;
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
