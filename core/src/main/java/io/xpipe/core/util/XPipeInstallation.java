package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;

import java.util.Optional;

public class XPipeInstallation {

    public static Optional<String> queryInstallationVersion(ShellProcessControl p) throws Exception {
        var executable = getInstallationExecutable(p);
        if (executable.isEmpty()) {
            return Optional.empty();
        }

        try (CommandProcessControl c = p.command(executable.get() + " version").start()) {
            return Optional.ofNullable(c.readOrThrow());
        }
    }

    public static boolean containsCompatibleInstallation(ShellProcessControl p, String version) throws Exception {
        var executable = getInstallationExecutable(p);
        if (executable.isEmpty()) {
            return false;
        }

        try (CommandProcessControl c = p.command(executable.get() + " version").start()) {
            return c.readOrThrow().equals(version);
        }
    }

    public static Optional<String> getInstallationExecutable(ShellProcessControl p) throws Exception {
        var installation = getDefaultInstallationBasePath(p);
        var executable = FileNames.join(installation, getDaemonExecutableInInstallationDirectory(p.getOsType()));
        var file = FileNames.join(installation, executable);
        try (CommandProcessControl c =
                p.command(p.getShellType().createFileExistsCommand(file)).start()) {
            return c.startAndCheckExit() ? Optional.of(file) : Optional.empty();
        }
    }

    public static String getDataBasePath(ShellProcessControl p) throws Exception {
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeSimpleCommand(p.getShellType().getPrintVariableCommand("userprofile"));
            return FileNames.join(base, "X-Pipe");
        } else {
            return FileNames.join("~", "xpipe");
        }
    }

    public static String getDefaultInstallationBasePath(ShellProcessControl p) throws Exception {
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeSimpleCommand(p.getShellType().getPrintVariableCommand("LOCALAPPDATA"));
            return FileNames.join(base, "X-Pipe");
        } else {
            return "/opt/xpipe";
        }
    }

    public static String getDaemonExecutableInInstallationDirectory(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "runtime", "bin", "xpiped.bat");
        } else {
            return FileNames.join("app/bin/xpiped");
        }
    }
}
