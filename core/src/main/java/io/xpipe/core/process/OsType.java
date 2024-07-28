package io.xpipe.core.process;

import io.xpipe.core.store.FileNames;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public interface OsType {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    MacOs MACOS = new MacOs();
    Bsd BSD = new Bsd();
    Solaris SOLARIS = new Solaris();

    static Local getLocal() {
        String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((osName.contains("mac")) || (osName.contains("darwin"))) {
            return MACOS;
        } else if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("nux")) {
            return LINUX;
        } else {
            throw new UnsupportedOperationException("Unknown operating system");
        }
    }

    String makeFileSystemCompatible(String name);

    List<String> determineInterestingPaths(ShellControl pc) throws Exception;

    String getHomeDirectory(ShellControl pc) throws Exception;

    String getFileSystemSeparator();

    String getName();

    String getTempDirectory(ShellControl pc) throws Exception;

    Map<String, String> getProperties(ShellControl pc) throws Exception;

    String determineOperatingSystemName(ShellControl pc) throws Exception;

    sealed interface Local extends OsType permits OsType.Windows, OsType.Linux, OsType.MacOs {

        String getId();

        default Any toAny() {
            return (Any) this;
        }
    }

    sealed interface Any extends OsType
            permits OsType.Windows, OsType.Linux, OsType.MacOs, OsType.Solaris, OsType.Bsd {}

    final class Windows implements OsType, Local, Any {

        @Override
        public String makeFileSystemCompatible(String name) {
            return name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\p{C}", "");
        }

        @Override
        public List<String> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = getHomeDirectory(pc);
            return List.of(
                    home,
                    FileNames.join(home, "Documents"),
                    FileNames.join(home, "Downloads"),
                    FileNames.join(home, "Desktop"));
        }

        @Override
        public String getHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(
                    pc.getShellDialect().getPrintEnvironmentVariableCommand("USERPROFILE"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "\\";
        }

        @Override
        public String getName() {
            return "Windows";
        }

        @Override
        public String getTempDirectory(ShellControl pc) throws Exception {
            var def = pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("TEMP"));
            if (!def.isBlank() && pc.getShellDialect().directoryExists(pc, def).executeAndCheck()) {
                return def;
            }

            var fallback = pc.executeSimpleStringCommand(
                    pc.getShellDialect().getPrintEnvironmentVariableCommand("LOCALAPPDATA"));
            if (!fallback.isBlank()
                    && pc.getShellDialect().directoryExists(pc, fallback).executeAndCheck()) {
                return fallback;
            }

            return def;
        }

        @Override
        public Map<String, String> getProperties(ShellControl pc) throws Exception {
            try (CommandControl c = pc.command("systeminfo").start()) {
                var text = c.readStdoutOrThrow();
                return PropertiesFormatsParser.parse(text, ":");
            }
        }

        @Override
        public String determineOperatingSystemName(ShellControl pc) {
            try {
                return pc.executeSimpleStringCommand("wmic os get Caption")
                                .lines()
                                .skip(1)
                                .collect(Collectors.joining())
                                .trim()
                        + " "
                        + pc.executeSimpleStringCommand("wmic os get Version")
                                .lines()
                                .skip(1)
                                .collect(Collectors.joining())
                                .trim();
            } catch (Throwable t) {
                // Just in case this fails somehow
                return "Windows";
            }
        }

        @Override
        public String getId() {
            return "windows";
        }
    }

    class Unix implements OsType {

        @Override
        public String makeFileSystemCompatible(String name) {
            // Technically the backslash is supported, but it causes all kinds of troubles, so we also exclude it
            return name.replaceAll("/\\\\", "_").replaceAll("\0", "");
        }

        @Override
        public List<String> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = getHomeDirectory(pc);
            return List.of(
                    home, "/home", FileNames.join(home, "Downloads"), FileNames.join(home, "Documents"), "/etc", "/tmp", "/var");
        }

        @Override
        public String getHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public String getName() {
            return "Linux";
        }

        @Override
        public String getTempDirectory(ShellControl pc) {
            return "/tmp/";
        }

        @Override
        public Map<String, String> getProperties(ShellControl pc) {
            return null;
        }

        @Override
        public String determineOperatingSystemName(ShellControl pc) throws Exception {
            String type = "Unknown";
            try (CommandControl c = pc.command("uname -o").start()) {
                var text = c.readStdoutDiscardErr();
                if (c.getExitCode() == 0) {
                    type = text.strip();
                }
            }

            String version = "?";
            try (CommandControl c = pc.command("uname -r").start()) {
                var text = c.readStdoutDiscardErr();
                if (c.getExitCode() == 0) {
                    version = text.strip();
                }
            }

            return type + " " + version;
        }
    }

    final class Linux extends Unix implements OsType, Local, Any {

        @Override
        public String getId() {
            return "linux";
        }

        @Override
        public String determineOperatingSystemName(ShellControl pc) throws Exception {
            try (CommandControl c = pc.command("lsb_release -a").start()) {
                var text = c.readStdoutDiscardErr();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, ":").getOrDefault("Description", "Unknown");
                }
            }

            try (CommandControl c = pc.command("cat /etc/*release").start()) {
                var text = c.readStdoutDiscardErr();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, "=").getOrDefault("PRETTY_NAME", "Unknown");
                }
            }

            return super.determineOperatingSystemName(pc);
        }
    }

    final class Solaris extends Unix implements Any {}

    final class Bsd extends Unix implements Any {}

    final class MacOs implements OsType, Local, Any {

        @Override
        public String getId() {
            return "macos";
        }

        @Override
        public String makeFileSystemCompatible(String name) {
            // Technically the backslash is supported, but it causes all kinds of troubles, so we also exclude it
            return name.replaceAll("[\\\\/:]", "_").replaceAll("\0", "");
        }

        @Override
        public List<String> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = getHomeDirectory(pc);
            return List.of(
                    home,
                    FileNames.join(home, "Downloads"),
                    FileNames.join(home, "Documents"),
                    FileNames.join(home, "Desktop"),
                    "/Applications",
                    "/Library",
                    "/System",
                    "/etc");
        }

        @Override
        public String getHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public String getName() {
            return "Mac";
        }

        @Override
        public String getTempDirectory(ShellControl pc) throws Exception {
            var found = pc.executeSimpleStringCommand(pc.getShellDialect().getPrintVariableCommand("TMPDIR"));

            // This variable is not defined for root users, so manually fix it. Why? ...
            if (found.isBlank()) {
                return "/tmp";
            }

            return found;
        }

        @Override
        public Map<String, String> getProperties(ShellControl pc) throws Exception {
            try (CommandControl c = pc.command("sw_vers").start()) {
                var text = c.readStdoutOrThrow();
                return PropertiesFormatsParser.parse(text, ":");
            }
        }

        @Override
        public String determineOperatingSystemName(ShellControl pc) throws Exception {
            var properties = getProperties(pc);
            var name = pc.executeSimpleStringCommand(
                    "awk '/SOFTWARE LICENSE AGREEMENT FOR macOS/' '/System/Library/CoreServices/Setup "
                            + "Assistant.app/Contents/Resources/en.lproj/OSXSoftwareLicense.rtf' | "
                            + "awk -F 'macOS ' '{print $NF}' | awk '{print substr($0, 0, length($0)-1)}'");
            // For preleases and others
            if (name.isBlank()) {
                name = "?";
            }
            return properties.get("ProductName") + " " + name + " " + properties.get("ProductVersion");
        }
    }
}
