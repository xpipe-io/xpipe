package io.xpipe.app.core;

import io.xpipe.core.OsType;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class AppSystemInfo {

    private static final Windows WINDOWS = new Windows();
    private static final Linux LINUX = new Linux();
    private static final MacOs MACOS = new MacOs();

    public static Windows getWindows() {
        if (OsType.getLocal() != OsType.WINDOWS) {
            throw new IllegalStateException();
        }

        return WINDOWS;
    }

    public static Linux getLinux() {
        if (OsType.getLocal() != OsType.LINUX) {
            throw new IllegalStateException();
        }

        return LINUX;
    }

    public static MacOs getMacOs() {
        if (OsType.getLocal() != OsType.MACOS) {
            throw new IllegalStateException();
        }

        return MACOS;
    }

    private static Path parsePath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        try {
            return Path.of(path);
        } catch (InvalidPathException ignored) {
            return null;
        }
    }

    public static final class Windows {

        private Path userHome;
        private Path localAppData;
        private Path temp;

        public Path getSystemRoot() {
            var root = AppSystemInfo.parsePath(System.getenv("SystemRoot"));
            if (root == null) {
                return Path.of("C:\\Windows");
            }
            return root;
        }

        public Path getTemp() {
            if (temp != null) {
                return temp;
            }

            var dir = AppSystemInfo.parsePath(System.getenv("TEMP"));
            if (dir == null) {
                dir = AppSystemInfo.parsePath(System.getenv("TMP"));
            }

            if (dir == null) {
                return (temp = getLocalAppData().resolve("Temp"));
            }

            // Don't use system temp dir
            if (dir.startsWith(Path.of("C:\\Windows"))) {
                return (temp = getLocalAppData().resolve("Temp"));
            }

            try {
                // Replace 8.3 filename
                dir = dir.toRealPath();
            } catch (Exception ignored) {
            }
            return (temp = dir);
        }

        public Path getProgramFiles() {
            var env = AppSystemInfo.parsePath(System.getenv("ProgramFiles"));
            if (env != null) {
                return env;
            }

            var def = Path.of("C:\\ProgramFiles");
            return def;
        }

        public Path getLocalAppData() {
            if (localAppData != null) {
                return localAppData;
            }

            var dir = AppSystemInfo.parsePath(System.getenv("LOCALAPPDATA"));
            if (dir != null) {
                try {
                    // Replace 8.3 filename
                    dir = dir.toRealPath();
                } catch (Exception ignored) {
                }
                return (localAppData = dir);
            }

            var def = getUserHome().resolve("AppData").resolve("Local");
            return def;
        }

        public Path getUserHome() {
            if (userHome != null) {
                return userHome;
            }

            var dir = AppSystemInfo.parsePath(System.getenv("USERPROFILE"));
            if (dir == null) {
                dir = AppSystemInfo.parsePath(System.getProperty("user.home"));
            }
            if (dir == null) {
                var username = System.getenv("USERNAME");
                if (username == null) {
                    username = System.getProperty("user.name");
                }
                if (username == null) {
                    username = "User";
                }
                dir = Path.of("C:\\Users\\" + username);
            }

            try {
                // Replace 8.3 filename
                userHome = dir.toRealPath();
            } catch (Exception ignored) {
                userHome = dir;
            }
            return dir;
        }
    }

    public static class Linux {

        public boolean isDebianBased() {
            return Files.exists(Path.of("/etc/debian_version"));
        }
    }

    public static class MacOs {}
}
