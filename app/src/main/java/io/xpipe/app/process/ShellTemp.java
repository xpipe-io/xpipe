package io.xpipe.app.process;

import io.xpipe.app.core.AppNames;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;

import java.io.IOException;

public class ShellTemp {

    private static FilePath getUserSpecificTempDataDirectoryPath(ShellControl proc) throws Exception {
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root is technically unique as only root will use /tmp
        if (proc.getOsType() != OsType.WINDOWS && proc.getOsType() != OsType.MACOS) {
            var temp = proc.getSystemTemporaryDirectory();
            var base = temp.join(AppNames.ofCurrent().getKebapName() + "-" + proc.view().user());
            return base;
        } else {
            var temp = proc.getSystemTemporaryDirectory();
            var base = temp.join(AppNames.ofCurrent().getKebapName());
            return base;
        }
    }

    private static void initUserSpecificTempDataDirectory(ShellControl proc) throws Exception {
        var base = getUserSpecificTempDataDirectoryPath(proc);
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root is technically unique as only root will use /tmp
        if (proc.getOsType() != OsType.WINDOWS && proc.getOsType() != OsType.MACOS) {
            proc.view().mkdir(base);
            // We have to make sure that we own this directory, chmod will fail if not
            // This command should work in all shells
            if (!proc.view().isRoot()) {
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
        } else {
            proc.view().mkdir(base);
        }
    }

    public static boolean checkSystemTempDirectory(ShellControl sc) throws Exception {
        // We can't do anything in a TTY as we use normal commands for the temp init
        if (sc.getTtyState() != ShellTtyState.NONE) {
            return false;
        }

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

        return hasValidTemp;
    }

    public static FilePath createSubTempDirectory(ShellControl sc, boolean hasValidTemp) throws Exception {
        if (!hasValidTemp) {
            return sc.getSystemTemporaryDirectory();
        }

        // When starting up multiple sessions to the same system, there might be race conditions here
        // This is quite inefficient but there is no way to synchronize access on a
        // specific system when multiple shell controls access it
        synchronized (ShellTemp.class) {
            var subTemp = getUserSpecificTempDataDirectoryPath(sc);
            var sessionFile = subTemp.join("xpipe-session-"
                    + AppProperties.get().getSessionId().toString().substring(0, 8));
            var newSession = !sc.view().fileExists(sessionFile);
            if (newSession) {
                clearTemp(sc);
                initUserSpecificTempDataDirectory(sc);
                try {
                    sc.view().touch(sessionFile);
                } catch (ProcessOutputException pex) {
                    if (!pex.getOutput().toLowerCase().contains("no space left on device")) {
                        throw pex;
                    }
                }
            }
            return subTemp;
        }
    }

    private static void clearTemp(ShellControl sc) throws Exception {
        var toClean = getUserSpecificTempDataDirectoryPath(sc);
        // Only clear local shell dir to not interfere with AppLocalTemp too much
        if (sc.isLocal()) {
            toClean = toClean.join("shell");
        }
        if (sc.view().directoryExists(toClean)) {
            clearFiles(sc, toClean);
        }
    }

    private static void clearFiles(ShellControl sc, FilePath dir) throws Exception {
        var d = sc.getShellDialect();
        if (d == ShellDialects.CMD) {
            sc.command(CommandBuilder.of().add("DEL", "/Q", "/F").addFile(dir)).executeAndCheck();
        } else if (ShellDialects.isPowershell(d)) {
            sc.command(CommandBuilder.of()
                            .add("Remove-Item",
                                    "-Recurse",
                                    "-Force")
                            .addFile(dir))
                    .executeAndCheck();
        } else {
            sc.command(CommandBuilder.of()
                            .add("rm", "-rf")
                            .addFile(dir))
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
        var base = proc.getSubTemporaryDirectory();
        var dir = base.join(sub);
        proc.view().mkdir(dir);
        return dir;
    }
}
