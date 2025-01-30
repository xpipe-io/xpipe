package io.xpipe.core.util;

import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileNames;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        return 21721 + offset;
    }

    private static String getPkgId() {
        return isStaging() ? "io.xpipe.xpipe-ptb" : "io.xpipe.xpipe";
    }

    public static Path getLocalBeaconAuthFile() {
        return Path.of(System.getProperty("java.io.tmpdir"), isStaging() ? "xpipe_ptb_auth" : "xpipe_auth");
    }

    public static String createExternalAsyncLaunchCommand(
            String installationBase, XPipeDaemonMode mode, String arguments, boolean restart) {
        var suffix = (arguments != null ? " " + arguments : "");
        var modeOption = mode != null ? " --mode " + mode.getDisplayName() : "";
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + installationBase + "/bin/xpiped\"" + modeOption + suffix + " & disown";
        } else if (OsType.getLocal().equals(OsType.MACOS)) {
            if (restart) {
                return "(sleep 1;open \"" + installationBase + "\" --args" + modeOption + suffix
                        + "</dev/null &>/dev/null) & disown";
            } else {
                return "open \"" + installationBase + "\" --args" + modeOption + suffix;
            }
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
        Path path = toRealPathIfPossible(
                Path.of(ProcessHandle.current().info().command().orElseThrow()));
        // Check if the process was started using a relative path, and adapt it if necessary
        if (!path.isAbsolute()) {
            path = toRealPathIfPossible(Path.of(System.getProperty("user.dir")).resolve(path));
        }

        var name = path.getFileName().toString();
        // Check if we launched the JVM via a start script instead of the native executable
        if (name.endsWith("java") || name.endsWith("java.exe")) {
            // If we are not an image, we are probably running in a development environment where we want to use the
            // working directory
            var isImage = ModuleHelper.isImage();
            if (!isImage) {
                return Path.of(System.getProperty("user.dir"));
            }
            return getLocalInstallationBasePathForJavaExecutable(path);
        } else {
            return getLocalInstallationBasePathForDaemonExecutable(path);
        }
    }

    private static Path toRealPathIfPossible(Path p) {
        try {
            // Under certain conditions, e.g. when running on a ramdisk, path resolution might fail.
            // This is however not a big problem in that case, so we ignore it
            return p.toRealPath();
        } catch (IOException e) {
            return p;
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

    public static Path getLocalExtensionsDirectory(Path path) {
        return OsType.getLocal().equals(OsType.MACOS)
                ? path.resolve("Contents").resolve("Resources").resolve("extensions")
                : path.resolve("extensions");
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
            return executable.getParent().getParent().getParent().getParent();
        } else {
            return executable.getParent().getParent().getParent();
        }
    }

    private static Path getLocalInstallationBasePathForDaemonExecutable(Path executable) {
        // Resolve root path of installation relative to executable in a JPackage installation
        if (OsType.getLocal().equals(OsType.MACOS)) {
            return executable.getParent().getParent().getParent();
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return executable.getParent().getParent();
        } else {
            return executable.getParent();
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

    public static Path getLocalBundledToolsDirectory() {
        Path path = getCurrentInstallationBasePath();

        // Check for development environment
        if (!ModuleHelper.isImage()) {
            if (OsType.getLocal().equals(OsType.WINDOWS)) {
                return path.resolve("dist").resolve("bundled_bin").resolve("windows");
            } else if (OsType.getLocal().equals(OsType.LINUX)) {
                return path.resolve("dist").resolve("bundled_bin").resolve("linux");
            } else {
                return path.resolve("dist").resolve("bundled_bin").resolve("osx");
            }
        }

        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return path.resolve("bundled");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return path.resolve("bundled");
        } else {
            return path.resolve("Contents").resolve("Resources").resolve("bundled");
        }
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
            return path.resolve("logo.ico");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return path.resolve("logo.png");
        } else {
            return path.resolve("Contents").resolve("Resources").resolve("logo.icns");
        }
    }

    public static String getLocalDefaultInstallationBasePath() {
        return getLocalDefaultInstallationBasePath(staging);
    }

    public static String getLocalDefaultInstallationBasePath(boolean stage) {
        String path;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var pg = System.getenv("ProgramFiles");
            var systemPath = Path.of(pg, stage ? "XPipe PTB" : "XPipe");
            if (Files.exists(systemPath)) {
                return systemPath.toString();
            }

            var base = System.getenv("LOCALAPPDATA");
            path = FileNames.join(base, stage ? "XPipe PTB" : "XPipe");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            path = stage ? "/opt/xpipe-ptb" : "/opt/xpipe";
        } else {
            path = stage ? "/Applications/XPipe PTB.app" : "/Applications/XPipe.app";
        }

        return path;
    }

    public static Path getLangPath() {
        if (!ModuleHelper.isImage()) {
            return getCurrentInstallationBasePath().resolve("lang");
        }

        var install = getCurrentInstallationBasePath();
        var type = OsType.getLocal();
        if (type.equals(OsType.WINDOWS)) {
            return install.resolve("lang");
        } else if (type.equals(OsType.LINUX)) {
            return install.resolve("lang");
        } else {
            return install.resolve("Contents").resolve("Resources").resolve("lang");
        }
    }

    public static Path getBundledFontsPath() {
        if (!ModuleHelper.isImage()) {
            return Path.of("dist", "fonts");
        }

        var install = getCurrentInstallationBasePath();
        var type = OsType.getLocal();
        if (type.equals(OsType.WINDOWS)) {
            return install.resolve("fonts");
        } else if (type.equals(OsType.LINUX)) {
            return install.resolve("fonts");
        } else {
            return install.resolve("Contents").resolve("Resources").resolve("fonts");
        }
    }

    public static String getDaemonDebugScriptPath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("scripts", "xpiped_debug.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("scripts", "xpiped_debug.sh");
        } else {
            return FileNames.join("Contents", "Resources", "scripts", "xpiped_debug.sh");
        }
    }

    public static String getDaemonDebugAttachScriptPath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("scripts", "xpiped_debug_attach.bat");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("scripts", "xpiped_debug_attach.sh");
        } else {
            return FileNames.join("Contents", "Resources", "scripts", "xpiped_debug_attach.sh");
        }
    }

    public static String getDaemonExecutablePath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("xpiped.exe");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("bin", "xpiped");
        } else {
            return FileNames.join("Contents", "MacOS", "xpiped");
        }
    }

    public static String getRelativeCliExecutablePath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FileNames.join("bin", "xpipe.exe");
        } else if (type.equals(OsType.LINUX)) {
            return FileNames.join("bin", "xpipe");
        } else {
            return FileNames.join("Contents", "MacOS", "xpipe");
        }
    }
}
