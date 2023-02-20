package io.xpipe.core.process;

import java.util.Locale;
import java.util.Map;

public interface OsType {

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

    String getName();

    String getTempDirectory(ShellProcessControl pc) throws Exception;

    String normalizeFileName(String file);

    Map<String, String> getProperties(ShellProcessControl pc) throws Exception;

    String determineOperatingSystemName(ShellProcessControl pc) throws Exception;

    static class Windows implements OsType {

        @Override
        public String getName() {
            return "Windows";
        }

        @Override
        public String getTempDirectory(ShellProcessControl pc) throws Exception {
            return pc.executeStringSimpleCommand(pc.getShellType().getPrintEnvironmentVariableCommand("TEMP"));
        }

        @Override
        public String normalizeFileName(String file) {
            return String.join("\\", file.split("[\\\\/]+"));
        }

        @Override
        public Map<String, String> getProperties(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c = pc.command("systeminfo").start()) {
                var text = c.readOrThrow();
                return PropertiesFormatsParser.parse(text, ":");
            }
        }

        @Override
        public String determineOperatingSystemName(ShellProcessControl pc) throws Exception {
            var properties = getProperties(pc);
            return properties.get("OS Name") + " "
                    + properties.get("OS Version").split(" ")[0];
        }
    }

    static class Linux implements OsType {

        @Override
        public String getTempDirectory(ShellProcessControl pc) throws Exception {
            return "/tmp/";
        }

        @Override
        public String normalizeFileName(String file) {
            return String.join("/", file.split("[\\\\/]+"));
        }

        @Override
        public String getName() {
            return "Linux";
        }

        @Override
        public Map<String, String> getProperties(ShellProcessControl pc) throws Exception {
            return null;
        }

        @Override
        public String determineOperatingSystemName(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c = pc.command("lsb_release -a").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, ":").getOrDefault("Description", null);
                }
            }

            try (CommandProcessControl c = pc.command("cat /etc/*release").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, "=").getOrDefault("PRETTY_NAME", null);
                }
            }

            String type = "Unknown";
            try (CommandProcessControl c = pc.command("uname -o").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    type = text.strip();
                }
            }

            String version = "?";
            try (CommandProcessControl c = pc.command("uname -r").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    version = text.strip();
                }
            }

            return type + " " + version;
        }
    }

    static class MacOs implements OsType {

        @Override
        public String getTempDirectory(ShellProcessControl pc) throws Exception {
            return pc.executeStringSimpleCommand(pc.getShellType().getPrintVariableCommand("TMPDIR"));
        }

        @Override
        public String normalizeFileName(String file) {
            return String.join("/", file.split("[\\\\/]+"));
        }

        @Override
        public String getName() {
            return "Mac";
        }

        @Override
        public Map<String, String> getProperties(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c =
                    pc.subShell(ShellDialects.BASH).command("sw_vers").start()) {
                var text = c.readOrThrow();
                return PropertiesFormatsParser.parse(text, ":");
            }
        }

        @Override
        public String determineOperatingSystemName(ShellProcessControl pc) throws Exception {
            var properties = getProperties(pc);
            var name = pc.executeStringSimpleCommand(
                    "awk '/SOFTWARE LICENSE AGREEMENT FOR macOS/' '/System/Library/CoreServices/Setup "
                            + "Assistant.app/Contents/Resources/en.lproj/OSXSoftwareLicense.rtf' | "
                            + "awk -F 'macOS ' '{print $NF}' | awk '{print substr($0, 0, length($0)-1)}'");
            return properties.get("ProductName") + " " + name + " " + properties.get("ProductVersion");
        }
    }
}
