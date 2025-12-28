package io.xpipe.app.core;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.LocalExec;
import io.xpipe.core.OsType;

import com.sun.jna.platform.win32.*;

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

    public abstract String getUser();

    public static final class Windows extends AppSystemInfo {

        private Path userHome;
        private Path localAppData;
        private Path roamingAppData;
        private Path temp;
        private Path downloads;
        private Path desktop;
        private Path documents;

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

        @Override
        public String getUser() {
            var username = System.getenv("USERNAME");
            if (username == null) {
                username = System.getProperty("user.name");
            }
            if (username == null) {
                username = "User";
            }
            return username;
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

            try {
                var r = Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_Downloads);
                // Replace 8.3 filename
                return (downloads = Path.of(r).toRealPath());
            } catch (Throwable e) {
                ErrorEventFactory.fromThrowable(e).handle();
                var fallback = getUserHome().resolve("Downloads");
                return (downloads = fallback);
            }
        }

        public Path getDocuments() {
            if (documents != null) {
                return documents;
            }

            try {
                var r = Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_Documents);
                // Replace 8.3 filename
                return (documents = Path.of(r).toRealPath());
            } catch (Throwable e) {
                ErrorEventFactory.fromThrowable(e).handle();
                var fallback = getUserHome().resolve("Documents");
                return (documents = fallback);
            }
        }

        @Override
        public Path getDesktop() {
            if (desktop != null) {
                return desktop;
            }

            try {
                var r = Shell32Util.getKnownFolderPath(KnownFolders.FOLDERID_Desktop);
                // Replace 8.3 filename
                return (desktop = Path.of(r).toRealPath());
            } catch (Throwable e) {
                ErrorEventFactory.fromThrowable(e).handle();
                var fallback = getUserHome().resolve("Desktop");
                return (desktop = fallback);
            }
        }
    }

    public static class Linux extends AppSystemInfo {

        private Path downloads;
        private Path desktop;
        private Boolean vm;

        public boolean isDebianBased() {
            return Files.exists(Path.of("/etc/debian_version"));
        }

        public boolean isVirtualMachine() {
            if (vm != null) {
                return vm;
            }

            var out = LocalExec.readStdoutIfPossible("cat", "/proc/cpuinfo");
            vm = out.map(s -> s.contains("hypervisor")).orElse(false);
            return vm;
        }

        @Override
        public String getUser() {
            var username = System.getProperty("user.name");
            if (username == null) {
                username = "user";
            }
            return username;
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
        public String getUser() {
            var username = System.getProperty("user.name");
            if (username == null) {
                username = "user";
            }
            return username;
        }

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
