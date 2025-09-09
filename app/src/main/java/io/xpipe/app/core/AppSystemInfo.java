package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public abstract class AppSystemInfo {

    private static final Windows WINDOWS = new Windows();
    private static final Linux LINUX = new Linux();
    private static final MacOs MACOS = new MacOs();

    public static AppSystemInfo ofCurrent() {
        return switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> ofLinux();
            case OsType.MacOs ignored -> ofMacOs();
            case OsType.Windows ignored -> ofWindows();
        };
    }

    public static Windows ofWindows() {
        if (OsType.ofLocal() != OsType.WINDOWS) {
            throw new IllegalStateException();
        }

        return WINDOWS;
    }

    public static Linux ofLinux() {
        if (OsType.ofLocal() != OsType.LINUX) {
            throw new IllegalStateException();
        }

        return LINUX;
    }

    public static MacOs ofMacOs() {
        if (OsType.ofLocal() != OsType.MACOS) {
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

    public abstract Path getUserHome();

    public abstract Path getDownloads();

    public abstract Path getDesktop();

    public abstract Path getTemp();

    public static final class Windows extends AppSystemInfo {

        private Path userHome;
        private Path localAppData;
        private Path roamingAppData;
        private Path temp;
        private Path downloads;
        private Path desktop;

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

        public Path getRoamingAppData() {
            if (roamingAppData != null) {
                return roamingAppData;
            }

            var dir = AppSystemInfo.parsePath(System.getenv("APPDATA"));
            if (dir != null) {
                try {
                    // Replace 8.3 filename
                    dir = dir.toRealPath();
                } catch (Exception ignored) {
                }
                return (roamingAppData = dir);
            }

            var def = getUserHome().resolve("AppData").resolve("Roaming");
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

        @Override
        public Path getDownloads() {
            if (downloads != null) {
                return downloads;
            }

            var fallback = getUserHome().resolve("Downloads");
            var shell = LocalShell.getLocalPowershell();
            if (shell.isEmpty()) {
                return (downloads = fallback);
            }

            try {
                return (downloads = Path.of(shell.get()
                        .command("(New-Object -ComObject Shell.Application).NameSpace('shell:Downloads').Self.Path")
                        .readStdoutOrThrow()));
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return (downloads = fallback);
            }
        }

        @Override
        public Path getDesktop() {
            if (desktop != null) {
                return desktop;
            }

            var fallback = getUserHome().resolve("Desktop");
            var shell = LocalShell.getLocalPowershell();
            if (shell.isEmpty()) {
                return (desktop = fallback);
            }

            try {
                return (desktop = Path.of(shell.get()
                        .command("[Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)")
                        .readStdoutOrThrow()));
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
                return (desktop = fallback);
            }
        }
    }

    public static class Linux extends AppSystemInfo {

        private Path downloads;
        private Path desktop;

        public boolean isDebianBased() {
            return Files.exists(Path.of("/etc/debian_version"));
        }

        @Override
        public Path getUserHome() {
            return Path.of(System.getProperty("user.home"));
        }

        @Override
        public Path getDownloads() {
            if (downloads != null) {
                return downloads;
            }

            try (var sc = LocalShell.getShell().start()) {
                var out = sc.command("xdg-user-dir DOWNLOAD").readStdoutIfPossible();
                if (out.isPresent() && !out.get().isBlank()) {
                    return (downloads = Path.of(out.get()));
                }
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }

            var fallback = getUserHome().resolve("Desktop");
            return (downloads = fallback);
        }

        @Override
        public Path getDesktop() {
            if (desktop != null) {
                return desktop;
            }

            try (var sc = LocalShell.getShell().start()) {
                var out = sc.command("xdg-user-dir DESKTOP").readStdoutIfPossible();
                if (out.isPresent() && !out.get().isBlank()) {
                    return (desktop = Path.of(out.get()));
                }
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }

            var fallback = getUserHome().resolve("Desktop");
            return (desktop = fallback);
        }

        @Override
        public Path getTemp() {
            return Path.of(System.getProperty("java.io.tmpdir"));
        }
    }

    public static class MacOs extends AppSystemInfo {

        @Override
        public Path getUserHome() {
            return Path.of(System.getProperty("user.home"));
        }

        @Override
        public Path getDownloads() {
            return getUserHome().resolve("Downloads");
        }

        @Override
        public Path getDesktop() {
            return getUserHome().resolve("Desktop");
        }

        @Override
        public Path getTemp() {
            return Path.of(System.getProperty("java.io.tmpdir"));
        }
    }
}
