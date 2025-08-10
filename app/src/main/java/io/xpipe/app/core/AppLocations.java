package io.xpipe.app.core;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public interface AppLocations {

    Windows WINDOWS = new Windows();
    Linux LINUX = new Linux();
    MacOs MACOS = new MacOs();

    static Windows getWindows() {
        return WINDOWS;
    }

    static Linux getLinux() {
        return LINUX;
    }

    static MacOs getMacOs() {
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

    final class Windows implements AppLocations {

        private Path userHome;

        public Path getSystemRoot() {
            var root = AppLocations.parsePath(System.getenv("SystemRoot"));
            if (root == null) {
                return Path.of("C:\\Windows");
            }
            return root;
        }

        public Path getTemp() {
            var env = AppLocations.parsePath(System.getenv("TEMP"));
            if (env == null) {
                env = AppLocations.parsePath(System.getenv("TMP"));
            }

            if (env == null) {
                return getLocalAppData().resolve("Temp");
            }

            // Don't use system temp dir
            if (env.startsWith(Path.of("C:\\Windows"))) {
                return getLocalAppData().resolve("Temp");
            }

            return env;
        }

        public Path getProgramFiles() {
            var env = AppLocations.parsePath(System.getenv("ProgramFiles"));
            if (env != null) {
                return env;
            }

            var def = Path.of("C:\\ProgramFiles");
            return def;
        }

        public Path getLocalAppData() {
            var env = AppLocations.parsePath(System.getenv("LOCALAPPDATA"));
            if (env != null) {
                return env;
            }

            var def = getUserHome().resolve("AppData").resolve("Local");
            return def;
        }

        public Path getUserHome() {
            if (userHome != null) {
                return userHome;
            }

            var dir = AppLocations.parsePath(System.getenv("USERPROFILE"));
            if (dir == null) {
                dir = AppLocations.parsePath(System.getProperty("user.home"));
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

    class Linux implements AppLocations {}

    final class MacOs implements AppLocations {}
}
