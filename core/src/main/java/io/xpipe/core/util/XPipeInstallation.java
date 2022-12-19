package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellProcessControl;

import java.util.List;

public class XPipeInstallation {

    public static String createExternalAsyncLaunchCommand(String installationBase, String arguments) {
        var suffix = (arguments != null ? arguments : "");
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + installationBase + "/app/bin/xpiped\" --external & disown" + suffix;
        }

        return FileNames.join(installationBase, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal())) + suffix;
    }

    public static String createExternalLaunchCommand(String command, String arguments) {
        var suffix = (arguments != null ? arguments : "");
        return "\"" + command + "\" --external" + suffix;
    }

    public static String getInstallationBasePathForCLI(ShellProcessControl p, String cliExecutable) throws Exception {
        var defaultInstallation =  getDefaultInstallationBasePath(p, true);
        if (cliExecutable == null) {
            return defaultInstallation;
        }

        if (p.getOsType().equals(OsType.LINUX) && cliExecutable.equals("/usr/bin/xpipe")) {
            return defaultInstallation;
        }

        if (FileNames.startsWith(cliExecutable, defaultInstallation)) {
            return defaultInstallation;
        }

        return FileNames.getParent(FileNames.getParent(cliExecutable));
    }

    public static String queryInstallationVersion(ShellProcessControl p, String exec) throws Exception {
        try (CommandProcessControl c = p.command(List.of(exec, "version")).start()) {
            return c.readOrThrow();
        } catch (ProcessOutputException ex) {
            return "?";
        }
    }

    public static String getInstallationExecutable(ShellProcessControl p, String installation) throws Exception {
        var executable = getDaemonExecutablePath(p.getOsType());
        var file = FileNames.join(installation, executable);
        return file;
    }

    public static String getDataBasePath(ShellProcessControl p) throws Exception {
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeStringSimpleCommand(p.getShellType().getPrintVariableCommand("userprofile"));
            return FileNames.join(base, ".xpipe");
        } else {
            return FileNames.join("~", ".xpipe");
        }
    }

    public static String getLocalDefaultInstallationBasePath() {
        var customHome = System.getenv("XPIPE_HOME");
        if (!customHome.isEmpty()) {
            return customHome;
        }

        String path = null;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var base = System.getenv("LOCALAPPDATA");
            path = FileNames.join(base, "X-Pipe");
        } else {
            path = "/opt/xpipe";
        }

        return path;
    }

    public static String getDefaultInstallationBasePath(ShellProcessControl p, boolean acceptPortable) throws Exception {
        if (acceptPortable) {
            var customHome = p.executeStringSimpleCommand(p.getShellType().getPrintVariableCommand("XPIPE_HOME"));
            if (!customHome.isEmpty()) {
                return customHome;
            }
        }

        String path = null;
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeStringSimpleCommand(p.getShellType().getPrintVariableCommand("LOCALAPPDATA"));
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
