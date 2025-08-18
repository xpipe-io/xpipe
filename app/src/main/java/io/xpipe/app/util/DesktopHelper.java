package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.update.AppDistributionType;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.awt.*;
import java.io.File;
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
                pb.directory(new File(System.getProperty("user.home")));
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
                pb.directory(new File(System.getProperty("user.home")));
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start();
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }
    }

    public static Path getDesktopDirectory() throws Exception {
        if (OsType.getLocal() == OsType.WINDOWS) {
            var shell = LocalShell.getLocalPowershell();
            if (shell.isEmpty()) {
                return Path.of(System.getProperty("user.home")).resolve("Desktop");
            }

            return Path.of(shell.get()
                    .command("[Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)")
                    .readStdoutOrThrow());
        } else if (OsType.getLocal() == OsType.LINUX) {
            try (var sc = LocalShell.getShell().start()) {
                var out = sc.command("xdg-user-dir DESKTOP").readStdoutIfPossible();
                if (out.isPresent()) {
                    return Path.of(out.get());
                }
            }
        }

        return Path.of(System.getProperty("user.home") + "/Desktop");
    }

    public static Path getDownloadsDirectory() throws Exception {
        if (OsType.getLocal() == OsType.WINDOWS) {
            var shell = LocalShell.getLocalPowershell();
            if (shell.isEmpty()) {
                return Path.of(System.getProperty("user.home")).resolve("Desktop");
            }

            return Path.of(shell.get()
                    .command("(New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path")
                    .readStdoutOrThrow());
        } else if (OsType.getLocal() == OsType.LINUX) {
            try (var sc = LocalShell.getShell().start()) {
                var out = sc.command("xdg-user-dir DOWNLOAD").readStdoutIfPossible();
                if (out.isPresent() && !out.get().isBlank()) {
                    return Path.of(out.get());
                }
            }
        }

        return Path.of(System.getProperty("user.home") + "/Downloads");
    }

    public static void browsePathRemote(ShellControl sc, FilePath path, FileKind kind) throws Exception {
        var d = sc.getShellDialect();
        switch (sc.getOsType()) {
            case OsType.Windows windows -> {
                // Explorer does not support single quotes, so use normal quotes
                if (kind == FileKind.DIRECTORY) {
                    sc.command(CommandBuilder.of().add("explorer").addQuoted(path.toString()))
                            .execute();
                } else {
                    sc.command(CommandBuilder.of().add("explorer", "/select,\"" + path.toString() + "\""))
                            .execute();
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
                var success = sc.executeSimpleBooleanCommand(dbus);
                if (success) {
                    return;
                }

                sc.command(CommandBuilder.of()
                                .add("xdg-open")
                                .addFile(kind == FileKind.DIRECTORY ? path : path.getParent()))
                        .execute();
            }
            case OsType.MacOs macOs -> {
                sc.command(CommandBuilder.of()
                                .add("open")
                                .addIf(kind == FileKind.DIRECTORY, "-R")
                                .addFile(path))
                        .execute();
            }
            case OsType.Bsd bsd -> {}
            case OsType.Solaris solaris -> {}
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
