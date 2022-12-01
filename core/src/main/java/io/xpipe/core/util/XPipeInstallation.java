package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;

public class XPipeInstallation {

    public static String getInstallationBasePathForCLI(ShellProcessControl p, String cliExecutable) throws Exception {
        var defaultInstallation =  getDefaultInstallationBasePath(p);
        if (p.getOsType().equals(OsType.LINUX) && cliExecutable.equals("/usr/bin/xpipe")) {
            return defaultInstallation;
        }

        if (FileNames.startsWith(cliExecutable, defaultInstallation)) {
            return defaultInstallation;
        }

        return FileNames.getParent(FileNames.getParent(cliExecutable));
    }

    public static String queryInstallationVersion(ShellProcessControl p, String exec) throws Exception {
        try (CommandProcessControl c = p.command(exec + " version").start()) {
            return c.readOrThrow();
        }
    }

    public static boolean containsCompatibleDefaultInstallation(ShellProcessControl p, String version) throws Exception {
        var defaultBase = getDefaultInstallationBasePath(p);
        var executable = getInstallationExecutable(p, defaultBase);
        if (!p.executeBooleanSimpleCommand(p.getShellType().createFileExistsCommand(executable))) {
            return false;
        }

        try (CommandProcessControl c = p.command(executable + " version").start()) {
            return c.readOrThrow().equals(version);
        }
    }

    public static String getInstallationExecutable(ShellProcessControl p, String installation) throws Exception {
        var executable = getDaemonExecutablePath(p.getOsType());
        var file = FileNames.join(installation, executable);
        return file;
    }

    public static String getDataBasePath(ShellProcessControl p) throws Exception {
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeSimpleCommand(p.getShellType().getPrintVariableCommand("userprofile"));
            return FileNames.join(base, "X-Pipe");
        } else {
            return FileNames.join("~", "xpipe");
        }
    }

    public static String getDefaultInstallationBasePath() throws Exception {
        try (ShellProcessControl pc = new LocalStore().create().start()) {
            return getDefaultInstallationBasePath(pc);
        }
    }

    public static String getDefaultInstallationBasePath(ShellProcessControl p) throws Exception {
        var customHome = p.executeSimpleCommand(p.getShellType().getPrintVariableCommand("XPIPE_HOME"));
        if (!customHome.isEmpty()) {
            return customHome;
        }

        String path = null;
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeSimpleCommand(p.getShellType().getPrintVariableCommand("LOCALAPPDATA"));
            path = FileNames.join(base, "X-Pipe");
        } else {
            path = "/opt/xpipe";
        }

        return path;
    }

    public static String getDaemonDebugScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug.bat");
        } else {
            return FileNames.join("app", "scripts", "xpiped_debug.sh");
        }
    }

    public static String getDaemonDebugAttachScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.bat");
        } else {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.sh");
        }
    }

    public static String getDaemonExecutablePath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "xpiped.exe");
        } else {
            return FileNames.join("app", "bin", "xpiped");
        }
    }
}
