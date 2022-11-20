package io.xpipe.core.util;

import io.xpipe.core.store.*;
import lombok.SneakyThrows;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public interface SupportedOs {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    Mac MAC = new Mac();

    static SupportedOs determine(ShellProcessControl pc) throws Exception {
        try (CommandProcessControl c = pc.command(pc.getShellType().createFileExistsCommand("C:\\pagefile.sys")).start()) {
            if (c.discardAndCheckExit()) {
                return WINDOWS;
            }
        }

        return LINUX;
    }

    Map<String, String> getProperties(ShellProcessControl pc) throws Exception;

    String determineOperatingSystemName(ShellProcessControl pc) throws Exception;

    @SneakyThrows
    public static SupportedOs getLocal() {
        try (ShellProcessControl pc = ShellStore.local().create().start()) {
            return determine(pc);
        }
    }

    Path getBaseInstallPath();

    UUID getSystemUUID(ShellProcessControl pc) throws Exception;

    static class Windows implements SupportedOs {

        @Override
        public Map<String, String> getProperties(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c =
                    pc.shell(ShellTypes.CMD).command("systeminfo").start()) {
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

    static class Linux implements SupportedOs {

        @Override
        public Map<String, String> getProperties(ShellProcessControl pc) throws Exception {
            return null;
        }

        @Override
        public String determineOperatingSystemName(ShellProcessControl pc) throws Exception {
            try (CommandProcessControl c =
                    pc.shell(ShellTypes.SH).command("lsb_release -a").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, ":").getOrDefault("Description", null);
                }
            }

            try (CommandProcessControl c =
                         pc.shell(ShellTypes.SH).command("cat /etc/*release").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    return PropertiesFormatsParser.parse(text, "=").getOrDefault("PRETTY_NAME", null);
                }
            }

            String type = "Unknown";
            try (CommandProcessControl c =
                         pc.shell(ShellTypes.SH).command("uname -o").start()) {
                var text = c.readOnlyStdout();
                if (c.getExitCode() == 0) {
                    type = text.strip();
                }
            }

            String version = "?";
            try (CommandProcessControl c =
                         pc.shell(ShellTypes.SH).command("uname -r").start()) {
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

    static class Mac implements SupportedOs {

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
