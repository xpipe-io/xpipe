package io.xpipe.app.process;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;

import java.io.IOException;

public class ShellTemp {

    public static FilePath createUserSpecificTempDataDirectory(ShellControl proc, String sub) throws Exception {
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root is technically unique as only root will use /tmp
        if (proc.getOsType() != OsType.WINDOWS && proc.getOsType() != OsType.MACOS) {
            var temp = proc.getSystemTemporaryDirectory();
            var base = temp.join(AppNames.ofCurrent().getKebapName() + "-" + proc.view().user());
            proc.view().mkdir(base);
            if (!proc.view().isRoot()) {
                // We have to make sure that we own this directory, chmod will fail if not
                // This command should work in all shells
                var hasChmod = proc.view().findProgram("chmod").isPresent();
                if (hasChmod) {
                    var chmodSuccess = proc.command("chmod 700 " + proc.getShellDialect().fileArgument(base)).executeAndCheck();
                    if (!chmodSuccess) {
                        throw new IOException("Unexpected directory ownership and permissions for " + base);
                    }
                }
            } else {
                var hasLs = proc.view().findProgram("ls").isPresent();
                if (hasLs) {
                    var lsOut = proc.command("ls -ldn " + proc.getShellDialect().fileArgument(base)).readStdoutIfPossible();
                    if (lsOut.isPresent()) {
                        var split = lsOut.get().split("\\s+");
                        if (split.length >= 3) {
                            if (!split[2].equals("0")) {
                                throw new IOException("Unexpected directory ownership for " + base);
                            }

                            var hasChmod = proc.view().findProgram("chmod").isPresent();
                            if (hasChmod) {
                                var chmodSuccess = proc.command("chmod 700 " + proc.getShellDialect().fileArgument(base)).executeAndCheck();
                                if (!chmodSuccess) {
                                    throw new IOException("Unexpected directory ownership and permissions for " + base);
                                }
                            }
                        }
                    }
                }

            }
            return sub != null ? base.join(sub) : base;
        } else {
            var temp = proc.getSystemTemporaryDirectory();
            var base = temp.join(AppNames.ofCurrent().getKebapName());
            return sub != null ? base.join(sub) : base;
        }
    }

    public static void checkTempDirectory(ShellControl sc) throws Exception {
        var d = sc.getShellDialect();
        var systemTemp = sc.getSystemTemporaryDirectory();
        var hasValidTemp = d.directoryExists(sc, systemTemp.toString()).executeAndCheck()
                && checkDirectoryPermissions(sc, systemTemp.toString());

        // We only really need temp for cmd
        // On various containers temp might be not available, but we can make it work
        if (!sc.isLocal() && sc.getShellDialect() == ShellDialects.CMD && !hasValidTemp) {
            throw ErrorEventFactory.expected(
                    new IOException("No permissions to access system temporary directory %s".formatted(systemTemp)));
        }

        if (hasValidTemp) {
            // When starting up multiple sessions to the same system, there might be race conditions here
            // This is quite inefficient but there is no way to synchronize access on a
            // specific system when multiple shell controls access it
            synchronized (ShellTemp.class) {
                var sessionFile = systemTemp.join("xpipe-session-"
                        + AppProperties.get().getSessionId().toString().substring(0, 8));
                var newSession = !sc.view().fileExists(sessionFile);
                if (newSession) {
                    clearTemp(sc);
                    try {
                        sc.view().touch(sessionFile);
                    } catch (ProcessOutputException pex) {
                        if (!pex.getOutput().toLowerCase().contains("no space left on device")) {
                            throw pex;
                        }
                    }
                }
            }
        }
    }

    public static void clearTemp(ShellControl sc) throws Exception {
        var systemTemp = sc.getSystemTemporaryDirectory();

        // The temp dir is a lot to clean on Windows potentially
        // Also, the wildcard remove is very slow in PowerShell
        var skipClear = OsType.ofLocal() == OsType.WINDOWS && sc.isLocal();
        if (!skipClear) {
            clearFiles(sc, systemTemp.join("xpipe-"));
        }
    }

    private static void clearFiles(ShellControl sc, FilePath prefix) throws Exception {
        var d = sc.getShellDialect();
        if (d == ShellDialects.CMD) {
            sc.command(CommandBuilder.of().add("DEL", "/Q", "/F").addQuoted(prefix.toString() + "*"))
                    .executeAndCheck();
        } else if (ShellDialects.isPowershell(d)) {
            sc.command(CommandBuilder.of()
                            .add("Get-ChildItem")
                            .addFile(prefix.getParent())
                            .add(
                                    "|",
                                    "Where-Object",
                                    "{-not $_.PSIsContainer}",
                                    "|",
                                    "Where-Object",
                                    "{$_.Name.StartsWith(\"" + prefix.getFileName() + "\")}",
                                    "|",
                                    "Remove-Item",
                                    "-Recurse",
                                    "-Force"))
                    .executeAndCheck();
        } else {
            sc.command(CommandBuilder.of()
                            .add("rm", "-f")
                            .add("\"" + prefix.toString() + "\"*")
                            .add("2>/dev/null"))
                    .executeAndCheck();
        }
    }

    private static boolean checkDirectoryPermissions(ShellControl proc, String dir) throws Exception {
        if (proc.getOsType() == OsType.WINDOWS) {
            return true;
        }

        var d = proc.getShellDialect();
        var fullAccess = proc.command("test -r %s && test -w %s && test -x %s"
                .formatted(d.fileArgument(dir), d.fileArgument(dir), d.fileArgument(dir))).executeAndCheck();
        if (!fullAccess) {
            return false;
        }

        return true;
    }

    public static FilePath getSubDirectory(ShellControl proc, String... sub) throws Exception {
        var base = proc.getSystemTemporaryDirectory();
        var dir = base.join(sub);
        // We assume that this directory does not exist yet and therefore don't perform any checks
        proc.getShellDialect().prepareUserTempDirectory(proc, dir.toString()).execute();
        return dir;
    }
}
