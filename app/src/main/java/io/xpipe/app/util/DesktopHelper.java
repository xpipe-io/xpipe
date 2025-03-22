package io.xpipe.app.util;

import io.xpipe.app.core.AppDistributionType;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FilePath;

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
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public static Path getDesktopDirectory() throws Exception {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return Path.of(LocalShell.getLocalPowershell()
                    .executeSimpleStringCommand("[Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)"));
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
            return Path.of(LocalShell.getLocalPowershell()
                    .executeSimpleStringCommand(
                            "(New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path"));
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
                var success = sc.executeSimpleBooleanCommand(dbus);
                if (success) {
                    return;
                }

                var file = new FilePath(path);
                sc.command(CommandBuilder.of()
                                .add("xdg-open")
                                .addFile(kind == FileKind.DIRECTORY ? file : file.getParent()))
                        .execute();
            }
            case OsType.MacOs macOs -> {
                sc.executeSimpleCommand("open " + (kind == FileKind.DIRECTORY ? "" : "-R ") + d.fileArgument(path));
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
            if (Desktop.getDesktop().isSupported(Desktop.Action.OPEN) && AppDistributionType.get() != AppDistributionType.WEBTOP) {
                try {
                    Desktop.getDesktop().open(file.toFile());
                    return;
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).expected().omitted(xdg).handle();
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
                    ErrorEvent.fromThrowable(e).expected().omitted(xdg).handle();
                }
            }

            if (xdg) {
                LocalExec.readStdoutIfPossible("xdg-open", file.getParent().toString());
            }
        });
    }
}
