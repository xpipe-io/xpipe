package io.xpipe.core;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class XPipeInstallation {

    private static final String STAGING_PROP = "io.xpipe.app.staging";

    @Getter
    private static final boolean staging = Optional.ofNullable(System.getProperty(STAGING_PROP))
            .map(Boolean::parseBoolean)
            .orElse(false);

    public static int getDefaultBeaconPort() {
        var offset = isStaging() ? 1 : 0;
        return 21721 + offset;
    }

    public static Path getLocalBeaconAuthFile() {
        return Path.of(System.getProperty("java.io.tmpdir"), isStaging() ? "xpipe_ptb_auth" : "xpipe_auth");
    }

    public static String createExternalAsyncLaunchCommand(
            String installationBase, XPipeDaemonMode mode, String arguments, boolean restart) {
        var suffix = (arguments != null ? " " + arguments : "");
        var modeOption = mode != null ? " -Dio.xpipe.app.mode=" + mode.getDisplayName() : "";
        if (OsType.getLocal().equals(OsType.LINUX)) {
            return "nohup \"" + installationBase + "/bin/xpiped\"" + modeOption + suffix
                    + "</dev/null >/dev/null 2>&1 & disown";
        } else if (OsType.getLocal().equals(OsType.MACOS)) {
            if (restart) {
                return "(sleep 1;open \"" + installationBase + "\" --args" + modeOption + suffix
                        + "</dev/null &>/dev/null) & disown";
            } else {
                return "open \"" + installationBase + "\" --args" + modeOption + suffix;
            }
        }

        return "\"" + FilePath.of(installationBase, XPipeInstallation.getDaemonExecutablePath(OsType.getLocal())) + "\""
                + modeOption + suffix;
    }

    public static String createExternalLaunchCommand(String command, String arguments, XPipeDaemonMode mode) {
        var suffix = (arguments != null ? " " + arguments : "");
        var modeOption = mode != null ? " -Dio.xpipe.app.mode=" + mode.getDisplayName() : "";
        return "\"" + command + "\"" + modeOption + suffix;
    }

    private static boolean isImage() {
        return XPipeInstallation.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getProtocol()
                .equals("jrt");
    }

    @SneakyThrows
    public static Path getCurrentInstallationBasePath() {
        var command = ProcessHandle.current().info().command();
        // We should always have a command associated with the current process, otherwise something went seriously wrong
        if (command.isEmpty()) {
            var javaHome = System.getProperty("java.home");
            var javaExec = toRealPathIfPossible(Path.of(javaHome, "bin", "java"));
            var path = getLocalInstallationBasePathForJavaExecutable(javaExec);
            return path;
        }

        // Resolve any possible links to a real path
        Path path = toRealPathIfPossible(Path.of(command.get()));
        // Check if the process was started using a relative path, and adapt it if necessary
        if (!path.isAbsolute()) {
            path = toRealPathIfPossible(Path.of(System.getProperty("user.dir")).resolve(path));
        }

        var name = path.getFileName().toString();
        // Check if we launched the JVM via a start script instead of the native executable
        if (name.endsWith("java") || name.endsWith("java.exe")) {
            // If we are not an image, we are probably running in a development environment where we want to use the
            // working directory
            var isImage = isImage();
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

    public static Path getLocalInstallationBasePathForCLI(String cliExecutable) {
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
            return path.getParent().getParent().getParent();
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return path.getParent().getParent();
        } else {
            return path.getParent().getParent();
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

    public static String getLocalDefaultCliExecutable() {
        Path path = isImage() ? getCurrentInstallationBasePath() : getLocalDefaultInstallationBasePath();
        return path.resolve(getRelativeCliExecutablePath(OsType.getLocal())).toString();
    }

    public static Path getLocalDefaultInstallationIcon() {
        Path path = getCurrentInstallationBasePath();

        // Check for development environment
        if (!isImage()) {
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
            return path.resolve("Contents").resolve("Resources").resolve("xpipe.icns");
        }
    }

    public static Path getLocalDefaultInstallationBasePath() {
        return getLocalDefaultInstallationBasePath(staging);
    }

    public static Path getLocalDefaultInstallationBasePath(boolean stage) {
        Path path;
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            var pg = System.getenv("ProgramFiles");
            var systemPath = Path.of(pg, stage ? "XPipe PTB" : "XPipe");
            if (Files.exists(systemPath)) {
                return systemPath;
            }

            var base = Path.of(System.getenv("LOCALAPPDATA"));
            path = base.resolve(stage ? "XPipe PTB" : "XPipe");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            path = Path.of(stage ? "/opt/xpipe-ptb" : "/opt/xpipe");
        } else {
            path = Path.of(stage ? "/Applications/XPipe PTB.app" : "/Applications/XPipe.app");
        }

        return path;
    }

    public static Path getLangPath() {
        if (!isImage()) {
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
        if (!isImage()) {
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
            return FilePath.of("scripts", "xpiped_debug.bat").toString();
        } else if (type.equals(OsType.LINUX)) {
            return FilePath.of("scripts", "xpiped_debug.sh").toString();
        } else {
            return FilePath.of("Contents", "Resources", "scripts", "xpiped_debug.sh")
                    .toString();
        }
    }

    public static String getDaemonDebugAttachScriptPath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FilePath.of("scripts", "xpiped_debug_attach.bat").toString();
        } else if (type.equals(OsType.LINUX)) {
            return FilePath.of("scripts", "xpiped_debug_attach.sh").toString();
        } else {
            return FilePath.of("Contents", "Resources", "scripts", "xpiped_debug_attach.sh")
                    .toString();
        }
    }

    public static String getDaemonExecutablePath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FilePath.of("xpiped.exe").toString();
        } else if (type.equals(OsType.LINUX)) {
            return FilePath.of("bin", "xpiped").toString();
        } else {
            return FilePath.of("Contents", "MacOS", "xpiped").toString();
        }
    }

    public static String getRelativeCliExecutablePath(OsType.Local type) {
        if (type.equals(OsType.WINDOWS)) {
            return FilePath.of("bin", "xpipe.exe").toString();
        } else if (type.equals(OsType.LINUX)) {
            return FilePath.of("bin", "xpipe").toString();
        } else {
            return FilePath.of("Contents", "MacOS", "xpipe").toString();
        }
    }
}
