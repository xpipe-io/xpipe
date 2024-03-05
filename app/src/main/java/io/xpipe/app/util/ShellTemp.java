package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileNames;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class ShellTemp {

    public static Path getLocalTempDataDirectory(String sub) {
        var temp = FileUtils.getTempDirectory().toPath().resolve("xpipe");
        if (OsType.getLocal().equals(OsType.LINUX)) {
            var user = System.getenv("USER");
            temp = temp.resolve(user != null ? user : "user");
        }
        return temp.resolve(sub);
    }

    public static String getUserSpecificTempDataDirectory(ShellControl proc, String sub) {
        if (OsType.getLocal().equals(OsType.LINUX) || OsType.getLocal().equals(OsType.MACOS)) {
            var user = System.getenv("USER");
            return FileNames.join("/tmp", "xpipe", user, sub);
        }
        var temp = proc.getSystemTemporaryDirectory();
        return FileNames.join(temp, "xpipe", sub);
    }

    public static void checkTempDirectory(ShellControl proc) throws Exception {
        var d = proc.getShellDialect();

        var systemTemp = proc.getOsType().getTempDirectory(proc);
        if (!d.directoryExists(proc, systemTemp).executeAndCheck() || !checkDirectoryPermissions(proc, systemTemp)) {
            throw ErrorEvent.expected(new IOException("No permissions to access %s".formatted(systemTemp)));
        }

        var home = proc.getOsType().getHomeDirectory(proc);
        if (!d.directoryExists(proc, home).executeAndCheck() || !checkDirectoryPermissions(proc, home)) {
            throw ErrorEvent.expected(new IOException("No permissions to access %s".formatted(home)));
        }

        // Always delete legacy directory and do not care whether it partially fails
        // This system xpipe temp directory might contain other files on the local machine, so only clear the exec
        d.deleteFileOrDirectory(proc, FileNames.join(systemTemp, "xpipe", "exec"))
                .executeAndCheck();
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
        var arr = Stream.concat(Stream.of(base), Arrays.stream(sub)).toArray(String[]::new);
        var dir = FileNames.join(arr);

        // We assume that this directory does not exist yet and therefore don't perform any checks
        proc.getShellDialect().prepareUserTempDirectory(proc, dir).execute();

        return dir;
    }
}
