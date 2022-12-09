package io.xpipe.core.process;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public interface OsType {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    Mac MAC = new Mac();

    String getName();

    String getTempDirectory(ShellProcessControl pc) throws Exception;

    String normalizeFileName(String file);

    Map<String, String> getProperties(ShellProcessControl pc) throws Exception;

    String determineOperatingSystemName(ShellProcessControl pc) throws Exception;

    public static OsType getLocal() {
        String osName = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((osName.contains("mac")) || (osName.contains("darwin"))) {
            return MAC;
        } else if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("nux")) {
            return LINUX;
        } else {
            throw new UnsupportedOperationException("Unknown operating system");
        }
    }

    Path getBaseInstallPath();

    UUID getSystemUUID(ShellProcessControl pc) throws Exception;

    static class Windows implements OsType {

        @Override
        public String getName() {
            return "Windows";
        }

        @Override
        public String getTempDirectory(ShellProcessControl pc) throws Exception {
            return pc.executeStringSimpleCommand(ShellTypes.CMD, ShellTypes.CMD.getPrintVariableCommand("TEMP"));
        }

        @Override
        public String normalizeFileName(String file) {
            return String.join("\\", file.split("[\\\\/]+"));
        }

        @Override
        public Map<String, String> getProperties(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c =
                    pc.subShell(ShellTypes.CMD).command("systeminfo").start()) {
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

        @Override
        public Path getBaseInstallPath() {
            return Path.of(System.getenv("LOCALAPPDATA"), "X-Pipe");
        }

        @Override
        public UUID getSystemUUID(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c = pc.command(
                    "reg query \"Computer\\\\HKEY_LOCAL_MACHINE\\\\SOFTWARE\\\\Microsoft\\\\Cryptography\" /v MachineGuid")) {
                var output = c.readOnlyStdout();
                return null;
            }
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
            try (CommandProcessControl c =
                    pc.command("lsb_release -a").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, ":").getOrDefault("Description", null);
                }
            }

            try (CommandProcessControl c =
                    pc.command("cat /etc/*release").start()) {
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

        @Override
        public Path getBaseInstallPath() {
            return Path.of("/opt/xpipe");
        }

        @Override
        public UUID getSystemUUID(ShellProcessControl pc) throws Exception {
            return null;
        }
    }

    static class Mac implements OsType {

        @Override
        public String getTempDirectory(ShellProcessControl pc) throws Exception {
            return pc.executeStringSimpleCommand(pc.getShellType().getPrintVariableCommand("TEMP"));
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
            return null;
        }

        @Override
        public String determineOperatingSystemName(ShellProcessControl pc) throws Exception {
            return null;
        }

        @Override
        public Path getBaseInstallPath() {
            return Path.of(System.getProperty("user.home"), "Application Support", "X-Pipe");
        }

        @Override
        public UUID getSystemUUID(ShellProcessControl pc) throws Exception {
            return null;
        }
    }
}
