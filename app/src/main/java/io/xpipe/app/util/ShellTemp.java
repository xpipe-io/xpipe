package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FilePath;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.stream.Stream;

public class ShellTemp {

    public static Path getLocalTempDataDirectory(String sub) {
        var temp = FileUtils.getTempDirectory().toPath().resolve("xpipe");
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root we will have a unique directory (in contrast to shell controls)
        if (OsType.getLocal().equals(OsType.LINUX)) {
            var user = System.getenv("USER");
            temp = temp.resolve(user != null ? user : "user");

            try {
                // We did not set this in earlier versions. If we are running as a different user, it might fail
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rwxrwxrwx"));
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().expected().handle();
            }
        }

        return temp.resolve(sub);
    }

    public static FilePath getUserSpecificTempDataDirectory(ShellControl proc, String sub) throws Exception {
        FilePath base;
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root it is technically unique as only root will use /tmp
        if (!proc.getOsType().equals(OsType.WINDOWS) && !proc.getOsType().equals(OsType.MACOS)) {
            var temp = proc.getSystemTemporaryDirectory();
            base = temp.join("xpipe");
            // We have to make sure that also other users can create files here
            // This command should work in all shells
            proc.command("chmod 777 " + proc.getShellDialect().fileArgument(base))
                    .executeAndCheck();
            var user = proc.getShellDialect().printUsernameCommand(proc).readStdoutOrThrow();
            base = temp.join(user);
        } else {
            var temp = proc.getSystemTemporaryDirectory();
            base = temp.join("xpipe");
        }
        return sub != null ? base.join(sub) : base;
    }

    public static void checkTempDirectory(ShellControl proc) throws Exception {
        var d = proc.getShellDialect();

        var systemTemp = proc.getSystemTemporaryDirectory();
        if (!d.directoryExists(proc, systemTemp.toString()).executeAndCheck()
                || !checkDirectoryPermissions(proc, systemTemp.toString())) {
            throw ErrorEvent.expected(new IOException("No permissions to access %s".formatted(systemTemp)));
        }

        // Always delete legacy directory and do not care whether it partially fails
        // This system xpipe temp directory might contain other files on the local machine, so only clear the exec
        d.deleteFileOrDirectory(proc, systemTemp.join("xpipe", "exec").toString())
                .executeAndCheck();
        var home = proc.getOsType().getHomeDirectory(proc);
        d.deleteFileOrDirectory(proc, FileNames.join(home, ".xpipe", "temp")).executeAndCheck();
        d.deleteFileOrDirectory(proc, FileNames.join(home, ".xpipe", "system_id"))
                .executeAndCheck();
    }

    private static boolean checkDirectoryPermissions(ShellControl proc, String dir) throws Exception {
        if (proc.getOsType().equals(OsType.WINDOWS)) {
            return true;
        }

        var d = proc.getShellDialect();
        return proc.executeSimpleBooleanCommand("test -r %s && test -w %s && test -x %s"
                .formatted(d.fileArgument(dir), d.fileArgument(dir), d.fileArgument(dir)));
    }

    public static String getSubDirectory(ShellControl proc, String... sub) throws Exception {
        var base = proc.getSystemTemporaryDirectory();
        var arr = Stream.concat(Stream.of(base.toString()), Arrays.stream(sub)).toArray(String[]::new);
        var dir = FileNames.join(arr);

        // We assume that this directory does not exist yet and therefore don't perform any checks
        proc.getShellDialect().prepareUserTempDirectory(proc, dir).execute();

        return dir;
    }
}
