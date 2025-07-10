package io.xpipe.app.process;

import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.util.ArrayList;
import java.util.List;
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
        var needsReplacement = split.stream().anyMatch(s -> !s.equals(makeFileSystemCompatible(s)));
        if (!needsReplacement) {
            return name;
        }

        var p = Pattern.compile("[^/\\\\]+");
        var m = p.matcher(name.toString());
        var replaced = m.replaceAll(matchResult -> makeFileSystemCompatible(matchResult.group()));
        return FilePath.of(replaced);
    }

    String makeFileSystemCompatible(String name);

    List<FilePath> determineInterestingPaths(ShellControl pc) throws Exception;

    String getUserHomeDirectory(ShellControl pc) throws Exception;

    String getFileSystemSeparator();

    final class Windows implements OsFileSystem {

        @Override
        public String makeFileSystemCompatible(String name) {
            return name.replaceAll("[<>:\"/\\\\|?*]", "_").replaceAll("\\p{C}", "");
        }

        @Override
        public List<FilePath> determineInterestingPaths(ShellControl pc) throws Exception {
            var home = pc.view().userHome();
            return List.of(home, home.join("Documents"), home.join("Downloads"), home.join("Desktop"));
        }

        @Override
        public String getUserHomeDirectory(ShellControl pc) throws Exception {
            var profile = pc.executeSimpleStringCommand(
                    pc.getShellDialect().getPrintEnvironmentVariableCommand("USERPROFILE"));
            if (!profile.isEmpty()) {
                return profile;
            }

            var name =
                    pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("USERNAME"));
            if (!name.isEmpty()) {
                return "C:\\Users\\" + name;
            }

            return "C:\\Users\\User";
        }

        @Override
        public String getFileSystemSeparator() {
            return "\\";
        }
    }

    class Unix implements OsFileSystem {

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
            var r = pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
            if (r.isBlank()) {
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
                return r;
            }
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }
    }

    final class MacOs implements OsFileSystem {

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
            return pc.executeSimpleStringCommand(pc.getShellDialect().getPrintEnvironmentVariableCommand("HOME"));
        }

        @Override
        public String getFileSystemSeparator() {
            return "/";
        }
    }
}
