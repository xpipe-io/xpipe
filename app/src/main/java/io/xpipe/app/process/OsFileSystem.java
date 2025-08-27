package io.xpipe.app.process;

import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public interface OsFileSystem {

    Windows WINDOWS = new Windows();
    Unix UNIX = new Unix();
    MacOs MACOS = new MacOs();

    static OsFileSystem ofLocal() {
        return of(OsType.getLocal());
    }

    static OsFileSystem of(OsType osType) {
        return switch (osType) {
            case OsType.Windows ignored -> WINDOWS;
            case OsType.Bsd ignored -> UNIX;
            case OsType.Linux ignored -> UNIX;
            case OsType.MacOs ignored -> MACOS;
            case OsType.Solaris ignored -> UNIX;
            default -> throw new IllegalStateException();
        };
    }

    default FilePath makeFileSystemCompatible(FilePath name) {
        var split = name.split();
        var needsReplacement = split.stream()
                .skip(hasMultipleRoots() && name.isAbsolute() ? 1 : 0)
                .anyMatch(s -> !s.equals(makeFileSystemCompatible(s)));
        if (!needsReplacement) {
            return name;
        }

        var p = Pattern.compile("[^/\\\\]+");
        var m = p.matcher(name.toString());
        var first = new AtomicBoolean(true);
        var replaced = m.replaceAll(matchResult -> {
            if (first.getAndSet(false) && hasMultipleRoots() && name.isAbsolute()) {
                return matchResult.group();
            }

            return makeFileSystemCompatible(matchResult.group());
        });
        return FilePath.of(replaced);
    }

    boolean isProbableFilePath(String s);

    String makeFileSystemCompatible(String name);

    List<FilePath> determineInterestingPaths(ShellControl pc) throws Exception;

    String getUserHomeDirectory(ShellControl pc) throws Exception;

    String getFileSystemSeparator();

    boolean hasMultipleRoots();

    final class Windows implements OsFileSystem {

        public boolean isProbableFilePath(String s) {
            if (s.length() >= 2 && s.charAt(1) == ':') {
                return true;
            }

            return false;
        }

        @Override
        public String makeFileSystemCompatible(String name) {
            var r = name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\p{C}", "");
            return r.strip();
        }

        @Override
        public List<FilePath> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = pc.view().userHome();
            return List.of(home, home.join("Documents"), home.join("Downloads"), home.join("Desktop"));
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            var profile = pc.view().getEnvironmentVariable("USERPROFILE");
            if (profile.isPresent()) {
                return profile.get();
            }

            var name = pc.view().getEnvironmentVariable("USERNAME");
            if (name.isPresent()) {
                return "C:\\Users\\" + name.get();
            }

            return "C:\\Users\\User";
        }

        @Override
        public String getFileSystemSeparator() {
            return "\\";
        }

        @Override
        public boolean hasMultipleRoots() {
            return true;
        }
    }

    class Unix implements OsFileSystem {

        public boolean isProbableFilePath(String s) {
            return s.startsWith("/");
        }

        @Override
        public String makeFileSystemCompatible(String name) {
            // Technically the backslash is supported, but it causes all kinds of troubles, so we also exclude it
            return name.replaceAll("[/\\\\]", "_").replaceAll("\0", "");
        }

        @Override
        public List<FilePath> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = pc.view().userHome();
            var list = new ArrayList<>(List.of(
                    home,
                    home.join("Downloads"),
                    home.join("Documents"),
                    FilePath.of("/etc"),
                    pc.getSystemTemporaryDirectory(),
                    FilePath.of("/var")));
            var parentHome = home.getParent();
            if (parentHome != null && !parentHome.toString().equals("/")) {
                list.add(3, parentHome);
            }
            return list;
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            var r = pc.view().getEnvironmentVariable("HOME");
            if (r.isEmpty()) {
                var user = pc.view().user();
                var eval = pc.command("eval echo ~" + user).readStdoutIfPossible();
                if (eval.isPresent() && !eval.get().isBlank()) {
                    return eval.get();
                }

                if (user.equals("root")) {
                    return "/root";
                } else {
                    return "/home/" + user;
                }
            } else {
                return r.get();
            }
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public boolean hasMultipleRoots() {
            return false;
        }
    }

    final class MacOs implements OsFileSystem {

        public boolean isProbableFilePath(String s) {
            return s.startsWith("/");
        }

        @Override
        public String makeFileSystemCompatible(String name) {
            // Technically the backslash is supported, but it causes all kinds of troubles, so we also exclude it
            return name.replaceAll("[\\\\/:]", "_").replaceAll("\0", "");
        }

        @Override
        public List<FilePath> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = pc.view().userHome();
            var list = List.of(
                    home,
                    home.join("Downloads"),
                    home.join("Documents"),
                    home.join("Desktop"),
                    FilePath.of("/Applications"),
                    FilePath.of("/Library"),
                    FilePath.of("/System"),
                    FilePath.of("/etc"),
                    FilePath.of("/tmp"));
            return list;
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            return pc.view().getEnvironmentVariableOrThrow("HOME");
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }

        @Override
        public boolean hasMultipleRoots() {
            return false;
        }
    }
}
