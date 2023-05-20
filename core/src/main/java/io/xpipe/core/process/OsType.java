package io.xpipe.core.process;

import io.xpipe.core.impl.FileNames;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public sealed interface OsType permits OsType.Windows, OsType.Linux, OsType.MacOs {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    MacOs MACOS = new MacOs();

    public static OsType getLocal() {
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

    default String getXPipeHomeDirectory(ShellControl pc) throws Exception {
        return FileNames.join(getHomeDirectory(pc), ".xpipe");
    }

    default String getSystemIdFile(ShellControl pc) throws Exception {
        return FileNames.join(getXPipeHomeDirectory(pc), "system_id");
    }

    String getHomeDirectory(ShellControl pc) throws Exception;

    String getFileSystemSeparator();

    String getName();

    String getTempDirectory(ShellControl pc) throws Exception;

    Map<String, String> getProperties(ShellControl pc) throws Exception;

    String determineOperatingSystemName(ShellControl pc) throws Exception;

    static final class Windows implements OsType {

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
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("TEMP"));
        }

        @Override
        public Map<String, String> getProperties(ShellControl pc) throws Exception {
            try (CommandControl c = pc.command("systeminfo").start()) {
                var text = c.readOrThrow();
                return PropertiesFormatsParser.parse(text, ":");
            }
        }

        @Override
        public String determineOperatingSystemName(ShellControl pc) throws Exception {
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
                return "Windows ?";
            }
        }
    }

    static final class Linux implements OsType {

        @Override
        public String getHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public String getTempDirectory(ShellControl pc) throws Exception {
            return "/tmp/";
        }

        @Override
        public String getName() {
            return "Linux";
        }

        @Override
        public Map<String, String> getProperties(ShellControl pc) throws Exception {
            return null;
        }

        @Override
        public String determineOperatingSystemName(ShellControl pc) throws Exception {
            try (CommandControl c = pc.command("lsb_release -a").start()) {
                var text = c.readStdoutDiscardErr();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, ":").getOrDefault("Description", null);
                }
            }

            try (CommandControl c = pc.command("cat /etc/*release").start()) {
                var text = c.readStdoutDiscardErr();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, "=").getOrDefault("PRETTY_NAME", null);
                }
            }

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

    static final class MacOs implements OsType {

        @Override
        public String getHomeDirectory(ShellControl pc) throws Exception {
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
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
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public String getName() {
            return "Mac";
        }

        @Override
        public Map<String, String> getProperties(ShellControl pc) throws Exception {
            try (CommandControl c =
                    pc.subShell(ShellDialects.BASH).command("sw_vers").start()) {
                var text = c.readOrThrow();
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
            return properties.get("ProductName") + " " + name + " " + properties.get("ProductVersion");
        }
    }
}
