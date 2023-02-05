package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.CommandProcessControl;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.process.ShellProcessControl;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class XPipeInstallation {

    public static String createExternalAsyncLaunchCommand(
            String installationBase, XPipeDaemonMode mode, String arguments) {
        var suffix = (arguments != null ? " " + arguments : "");
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + installationBase + "/app/bin/xpiped\" --mode " + mode.getDisplayName() + suffix
                    + " & disown";
        } else if (OsType.getLocal().equals(OsType.MAC)) {
            return "open \"" + installationBase + "\" --args --mode " + mode.getDisplayName() + suffix;
        }

        return "\"" + FileNames.join(installationBase, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()))
                + "\" --mode " + mode.getDisplayName() + suffix;
    }

    public static String createExternalLaunchCommand(String command, String arguments, XPipeDaemonMode mode) {
        var suffix = (arguments != null ? " " + arguments : "");
        return "\"" + command + "\" --mode " + mode.getDisplayName() + suffix;
    }

    @SneakyThrows
    public static Path getLocalInstallationBasePath() {
        Path path =
                Path.of(ProcessHandle.current().info().command().orElseThrow()).toRealPath();
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path).toRealPath();
        }

        var name = path.getFileName().toString();
        if (name.endsWith("java") || name.endsWith("java.exe")) {
            var isImage = ModuleHelper.isImage();
            if (!isImage) {
                return Path.of(System.getProperty("user.dir"));
            }
            return getLocalInstallationBasePathForJavaExecutable(path);
        } else {
            return getLocalInstallationBasePathForDaemonExecutable(path);
        }
    }

    public static boolean isInstallationDistribution() {
        var base = getLocalInstallationBasePath();
        if (OsType.getLocal().equals(OsType.MAC)) {
            if (!base.toString().equals(getLocalDefaultInstallationBasePath(false))) {
                return false;
            }

            try {
                var process = new ProcessBuilder("pkgutil", "--pkg-info", "io.xpipe.xpipe")
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
                process.waitFor();
                return process.exitValue() == 0;
            } catch (Exception ex) {
                return false;
            }
        } else {
            var file = base.resolve("installation");
            return Files.exists(file);
        }
    }

    public static Path getLocalDynamicLibraryDirectory() {
        Path path = getLocalInstallationBasePath();
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return path.resolve("app").resolve("runtime").resolve("bin");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return path.resolve("app").resolve("lib").resolve("runtime").resolve("lib");
        } else {
            return path.resolve("Contents")
                    .resolve("runtime")
                    .resolve("Contents")
                    .resolve("Home")
                    .resolve("lib");
        }
    }

    public static Path getLocalExtensionsDirectory() {
        Path path = getLocalInstallationBasePath();
        return OsType.getLocal().equals(OsType.MAC)
                ? path.resolve("Contents").resolve("Resources").resolve("extensions")
                : path.resolve("app").resolve("extensions");
    }

    private static Path getLocalInstallationBasePathForJavaExecutable(Path executable) {
        if (OsType.getLocal().equals(OsType.MAC)) {
            return executable
                    .getParent()
                    .getParent()
                    .getParent()
                    .getParent()
                    .getParent()
                    .getParent();
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return executable.getParent().getParent().getParent().getParent().getParent();
        } else {
            return executable.getParent().getParent().getParent().getParent();
        }
    }

    private static Path getLocalInstallationBasePathForDaemonExecutable(Path executable) {
        if (OsType.getLocal().equals(OsType.MAC)) {
            return executable.getParent().getParent().getParent();
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return executable.getParent().getParent().getParent();
        } else {
            return executable.getParent().getParent();
        }
    }

    public static String getLocalInstallationBasePathForCLI(String cliExecutable) throws Exception {
        var defaultInstallation = getLocalDefaultInstallationBasePath(true);
        if (cliExecutable == null) {
            return defaultInstallation;
        }

        if (OsType.getLocal().equals(OsType.LINUX) && cliExecutable.equals("/usr/bin/xpipe")) {
            return defaultInstallation;
        }

        if (FileNames.startsWith(cliExecutable, defaultInstallation)) {
            return defaultInstallation;
        }

        if (OsType.getLocal().equals(OsType.MAC)) {
            return FileNames.getParent(FileNames.getParent(FileNames.getParent(cliExecutable)));
        } else {
            return FileNames.getParent(FileNames.getParent(cliExecutable));
        }
    }

    public static String queryLocalInstallationVersion(String exec) throws Exception {
        var process = new ProcessBuilder(exec, "version")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        var v = new String(process.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        process.waitFor();
        return v;
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

    public static Path getLocalDefaultInstallationIcon() {
        Path path = getLocalInstallationBasePath();

        // Check for development environment
        if (!ModuleHelper.isImage()) {
            if (OsType.getLocal().equals(OsType.WINDOWS)) {
                return path.resolve("dist").resolve("logo").resolve("logo.ico");
            } else if (OsType.getLocal().equals(OsType.LINUX)) {
                return path.resolve("dist").resolve("logo").resolve("logo.png");
            } else {
                return path.resolve("dist").resolve("logo").resolve("logo.icns");
            }
        }

        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return path.resolve("app").resolve("logo.ico");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return path.resolve("logo.png");
        } else {
            return path.resolve("Contents")
                    .resolve("Resources")
                    .resolve("logo.icns");
        }
    }

    public static String getLocalDefaultInstallationBasePath(boolean acceptCustomHome) {
        var customHome = System.getenv("XPIPE_HOME");
        if (customHome != null && !customHome.isEmpty() && acceptCustomHome) {
            return customHome;
        }

        String path = null;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var base = System.getenv("LOCALAPPDATA");
            path = FileNames.join(base, "X-Pipe");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            path = "/opt/xpipe";
        } else {
            path = "/Applications/X-Pipe.app";
        }

        return path;
    }

    public static String getDefaultInstallationBasePath(ShellProcessControl p, boolean acceptPortable)
            throws Exception {
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
        } else if (p.getOsType().equals(OsType.LINUX)) {
            path = "/opt/xpipe";
        } else {
            path = "/Applications/X-Pipe.app";
        }

        return path;
    }

    public static String getDaemonDebugScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "scripts", "xpiped_debug.sh");
        } else {
            return FileNames.join("Content", "Resources", "scripts", "xpiped_debug.sh");
        }
    }

    public static String getDaemonDebugAttachScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.sh");
        } else {
            return FileNames.join("Content", "Resources", "scripts", "xpiped_debug_attach.sh");
        }
    }

    public static String getDaemonExecutablePath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "xpiped.exe");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "bin", "xpiped");
        } else {
            return FileNames.join("Content", "MacOS", "xpiped");
        }
    }
}
