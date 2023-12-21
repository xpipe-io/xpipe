package io.xpipe.core.util;

import io.xpipe.core.process.*;
import io.xpipe.core.store.FileNames;
import lombok.Getter;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class XPipeInstallation {

    public static final String DATA_DIR_PROP = "io.xpipe.app.dataDir";
    private static final String STAGING_PROP = "io.xpipe.app.staging";

    @Getter
    private static final boolean staging = Optional.ofNullable(System.getProperty(STAGING_PROP))
            .map(Boolean::parseBoolean)
            .orElse(false);

    public static int getDefaultBeaconPort() {
        var offset = isStaging() ? 1 : 0;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return 21721 + offset;
        } else {
            return 21721 + 2 + offset;
        }
    }

    public static Path getDataDir() {
        if (System.getProperty(DATA_DIR_PROP) != null) {
            try {
                return Path.of(System.getProperty(DATA_DIR_PROP));
            } catch (InvalidPathException ignored) {
            }
        }

        return Path.of(System.getProperty("user.home"), isStaging() ? ".xpipe-ptb" : ".xpipe");
    }

    public static String getDataDir(ShellControl p) throws Exception {
        var name = isStaging() ? ".xpipe-ptb" : ".xpipe";
        var dir = p.getOsType().getHomeDirectory(p);
        return FileNames.join(dir, name);
    }

    private static String getPkgId() {
        return isStaging() ? "io.xpipe.xpipe-ptb" : "io.xpipe.xpipe";
    }

    public static String createExternalAsyncLaunchCommand(
            String installationBase, XPipeDaemonMode mode, String arguments) {
        var suffix = (arguments != null ? " " + arguments : "");
        var modeOption = mode != null ? " --mode " + mode.getDisplayName() : "";
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + installationBase + "/app/bin/xpiped\"" + modeOption + suffix
                    + " & disown";
        } else if (OsType.getLocal().equals(OsType.MACOS)) {
            return "open \"" + installationBase + "\" --args" + modeOption + suffix;
        }

        return "\"" + FileNames.join(installationBase, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal()))
                + "\"" + modeOption + suffix;
    }

    public static String createExternalLaunchCommand(String command, String arguments, XPipeDaemonMode mode) {
        var suffix = (arguments != null ? " " + arguments : "");
        var modeOption = mode != null ? " --mode " + mode.getDisplayName() : null;
        return "\"" + command + "\"" + modeOption + suffix;
    }

    @SneakyThrows
    public static Path getCurrentInstallationBasePath() {
        // We should always have a command associated with the current process, otherwise something went seriously wrong
        // Resolve any possible links to a real path
        Path path = Path.of(ProcessHandle.current().info().command().orElseThrow()).toRealPath();
        // Check if the process was started using a relative path, and adapt it if necessary
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path).toRealPath();
        }

        var name = path.getFileName().toString();
        // Check if we launched the JVM via a start script instead of the native executable
        if (name.endsWith("java") || name.endsWith("java.exe")) {
            // If we are not an image, we are probably running in a development environment where we want to use the working directory
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
        var base = getCurrentInstallationBasePath();
        if (OsType.getLocal().equals(OsType.MACOS)) {
            if (!base.toString().equals(getLocalDefaultInstallationBasePath())) {
                return false;
            }

            try {
                var process = new ProcessBuilder("pkgutil", "--pkg-info", getPkgId())
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
        Path path = getCurrentInstallationBasePath();
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

    public static Path getLocalExtensionsDirectory(Path path) {
        return OsType.getLocal().equals(OsType.MACOS)
                ? path.resolve("Contents").resolve("Resources").resolve("extensions")
                : path.resolve("app").resolve("extensions");
    }

    private static Path getLocalInstallationBasePathForJavaExecutable(Path executable) {
        // Resolve root path of installation relative to the java executable in a JPackage installation
        if (OsType.getLocal().equals(OsType.MACOS)) {
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
        // Resolve root path of installation relative to executable in a JPackage installation
        if (OsType.getLocal().equals(OsType.MACOS)) {
            return executable.getParent().getParent().getParent();
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return executable.getParent().getParent().getParent();
        } else {
            return executable.getParent().getParent();
        }
    }

    public static String getLocalInstallationBasePathForCLI(String cliExecutable) {
        var defaultInstallation = getLocalDefaultInstallationBasePath();

        // Can be empty in development mode
        if (cliExecutable == null) {
            return defaultInstallation;
        }

        if (OsType.getLocal().equals(OsType.LINUX) && cliExecutable.equals("/usr/bin/xpipe")) {
            return defaultInstallation;
        }

        var path = Path.of(cliExecutable);
        if (OsType.getLocal().equals(OsType.MACOS)) {
            return path.getParent().getParent().getParent().toString();
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return path.getParent().getParent().getParent().toString();
        } else {
            return path.getParent().getParent().getParent().toString();
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

    public static String queryInstallationVersion(ShellControl p, String exec) throws Exception {
        try (CommandControl c = p.command(List.of(exec, "version")).start()) {
            return c.readStdoutOrThrow();
        } catch (ProcessOutputException ex) {
            return "?";
        }
    }

    public static String getInstallationExecutable(ShellControl p, String installation) {
        var executable = getDaemonExecutablePath(p.getOsType());
        return FileNames.join(installation, executable);
    }

    public static String getLocalDefaultCliExecutable() {
        Path path = ModuleHelper.isImage()
                ? getCurrentInstallationBasePath()
                : Path.of(getLocalDefaultInstallationBasePath());
        return path.resolve(getRelativeCliExecutablePath(OsType.getLocal())).toString();
    }

    public static Path getLocalDefaultInstallationIcon() {
        Path path = getCurrentInstallationBasePath();

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
            return path.resolve("Contents").resolve("Resources").resolve("logo.icns");
        }
    }

    public static String getLocalDefaultInstallationBasePath() {
        String path;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var base = System.getenv("LOCALAPPDATA");
            path = FileNames.join(base, isStaging() ? "XPipe PTB" : "XPipe");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            path = isStaging() ? "/opt/xpipe-ptb" : "/opt/xpipe";
        } else {
            path = isStaging() ? "/Applications/XPipe PTB.app" : "/Applications/XPipe.app";
        }

        return path;
    }

    public static String getDefaultInstallationBasePath(ShellControl p) throws Exception {
        String path;
        if (p.getOsType().equals(OsType.WINDOWS)) {
            var base = p.executeSimpleStringCommand(p.getShellDialect().getPrintVariableCommand("LOCALAPPDATA"));
            path = FileNames.join(base, isStaging() ? "XPipe PTB" : "XPipe");
        } else if (p.getOsType().equals(OsType.LINUX)) {
            path = isStaging() ? "/opt/xpipe-ptb" : "/opt/xpipe";
        } else {
            path = isStaging() ? "/Applications/XPipe PTB.app" : "/Applications/XPipe.app";
        }

        return path;
    }

    public static String getDaemonDebugScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "scripts", "xpiped_debug.sh");
        } else {
            return FileNames.join("Contents", "Resources", "scripts", "xpiped_debug.sh");
        }
    }

    public static String getDaemonDebugAttachScriptPath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "scripts", "xpiped_debug_attach.sh");
        } else {
            return FileNames.join("Contents", "Resources", "scripts", "xpiped_debug_attach.sh");
        }
    }

    public static String getDaemonExecutablePath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("app", "xpiped.exe");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("app", "bin", "xpiped");
        } else {
            return FileNames.join("Contents", "MacOS", "xpiped");
        }
    }

    public static String getRelativeCliExecutablePath(OsType type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("cli", "bin", "xpipe.exe");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("cli", "bin", "xpipe");
        } else {
            return FileNames.join("Contents", "MacOS", "xpipe");
        }
    }
}
