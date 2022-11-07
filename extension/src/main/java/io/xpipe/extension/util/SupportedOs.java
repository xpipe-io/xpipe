package io.xpipe.extension.util;

import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Path;
import java.util.UUID;

public interface SupportedOs {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    Mac MAC = new Mac();

    public static SupportedOs get() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return WINDOWS;
        } else if (SystemUtils.IS_OS_LINUX) {
            return LINUX;
        } else if (SystemUtils.IS_OS_MAC) {
            return MAC;
        } else {
            throw new UnsupportedOperationException("Unsupported operating system");
        }
    }

    Path getBaseInstallPath();

    UUID getSystemUUID();

    static class Windows implements SupportedOs {

        @Override
        public Path getBaseInstallPath() {
            return Path.of(System.getenv("LOCALAPPDATA"), "X-Pipe");
        }

        @Override
        public UUID getSystemUUID() {
            var s = WindowsRegistry.readRegistry(
                            "Computer\\HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Cryptography", "MachineGuid")
                    .orElse(null);
            if (s == null) {
                return null;
            }

            return UUID.fromString(s);
        }
    }

    static class Linux implements SupportedOs {

        @Override
        public Path getBaseInstallPath() {
            return Path.of("/opt/xpipe");
        }

        @Override
        public UUID getSystemUUID() {
            return null;
        }
    }

    static class Mac implements SupportedOs {

        @Override
        public Path getBaseInstallPath() {
            return Path.of(System.getProperty("user.home"), "Application Support", "X-Pipe");
        }

        @Override
        public UUID getSystemUUID() {
            return null;
        }
    }
}
