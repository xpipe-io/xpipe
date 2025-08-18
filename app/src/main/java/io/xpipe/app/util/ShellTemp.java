package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

public class ShellTemp {

    public static Path getLocalTempDataDirectory(String sub) {
        var temp = FileUtils.getTempDirectory().toPath().resolve("xpipe");
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root we will have a unique directory (in contrast to shell controls)
        if (OsType.getLocal().equals(OsType.LINUX)) {
            var user = System.getenv("USER");
            temp = temp.resolve(user != null ? user : "user");

            try {
                FileUtils.forceMkdir(temp.toFile());
                // We did not set this in earlier versions. If we are running as a different user, it might fail
                Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rwxrwxrwx"));
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().expected().handle();
            }
        }

        return sub != null ? temp.resolve(sub) : temp;
    }

    public static FilePath createUserSpecificTempDataDirectory(ShellControl proc, String sub) throws Exception {
        FilePath base;
        // On Windows and macOS, we already have user specific temp directories
        // Even on macOS as root it is technically unique as only root will use /tmp
        if (!proc.getOsType().equals(OsType.WINDOWS) && !proc.getOsType().equals(OsType.MACOS)) {
            var temp = proc.getSystemTemporaryDirectory();
            base = temp.join("xpipe");
            proc.command(proc.getShellDialect().getMkdirsCommand(base.toString()))
                    .execute();
            // We have to make sure that also other users can create files here
            // This command should work in all shells
            proc.command("chmod 777 " + proc.getShellDialect().fileArgument(base))
                    .executeAndCheck();
            var user = proc.getShellDialect().printUsernameCommand(proc).readStdoutOrThrow();
            base = base.join(user);
        } else {
            var temp = proc.getSystemTemporaryDirectory();
            base = temp.join("xpipe");
        }
        return sub != null ? base.join(sub) : base;
    }

    public static void checkTempDirectory(ShellControl proc) throws Exception {
        var d = proc.getShellDialect();

        // We only really need temp for cmd
        // On various containers the temp might be not available, but we can make it work
        if (!proc.isLocal() && proc.getShellDialect() != ShellDialects.CMD) {
            return;
        }

        var systemTemp = proc.getSystemTemporaryDirectory();
        if (!d.directoryExists(proc, systemTemp.toString()).executeAndCheck()
                || !checkDirectoryPermissions(proc, systemTemp.toString())) {
            throw ErrorEventFactory.expected(
                    new IOException("No permissions to access system temporary directory %s".formatted(systemTemp)));
        }

        // We don't do this anymore, we hope that all the legacy directories have been cleared now

        // Always delete legacy directory and do not care whether it partially fails
        // This system xpipe temp directory might contain other files on the local machine, so only clear the exec
        //        d.deleteFileOrDirectory(proc, systemTemp.join("xpipe", "exec").toString()).executeAndCheck();
        //        var home = proc.getOsType().getHomeDirectory(proc);
        //        d.deleteFileOrDirectory(proc, FilePath.of(home, ".xpipe", "temp")).executeAndCheck();
        //        d.deleteFileOrDirectory(proc, FilePath.of(home, ".xpipe", "system_id")).executeAndCheck();
    }

    private static boolean checkDirectoryPermissions(ShellControl proc, String dir) throws Exception {
        if (proc.getOsType().equals(OsType.WINDOWS)) {
            return true;
        }

        var d = proc.getShellDialect();
        return proc.executeSimpleBooleanCommand("test -r %s && test -w %s && test -x %s"
                .formatted(d.fileArgument(dir), d.fileArgument(dir), d.fileArgument(dir)));
    }

    public static FilePath getSubDirectory(ShellControl proc, String... sub) throws Exception {
        var base = proc.getSystemTemporaryDirectory();
        var dir = base.join(sub);
        // We assume that this directory does not exist yet and therefore don't perform any checks
        proc.getShellDialect().prepareUserTempDirectory(proc, dir.toString()).execute();
        return dir;
    }
}
