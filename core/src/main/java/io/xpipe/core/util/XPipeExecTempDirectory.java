package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellControl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

public class XPipeExecTempDirectory {

    private static final Set<UUID> usedSystems = new CopyOnWriteArraySet<>();

    public static String getSystemTempDirectory(ShellControl proc) throws Exception {
        return proc.getOsType().getTempDirectory(proc);
    }

    public static synchronized String initXPipeTempDirectory(ShellControl proc) throws Exception {
        var d = proc.getShellDialect();
        var tempBase = proc.getOsType().getTempDirectory(proc);
        var xpipeTemp = FileNames.join(tempBase, "xpipe");

        // Check permissions for system temp directory
        if (!checkDirectoryPermissions(proc, tempBase)) {
            var home = proc.getOsType().getHomeDirectory(proc);

            // We assume that this exists now as the systemid should have been created in this
            var xpipeHome = FileNames.join(home, ".xpipe");

            if (!d.directoryExists(proc, xpipeHome).executeAndCheck() || !checkDirectoryPermissions(proc, xpipeHome)) {
                throw new IOException("No permissions to create scripts in either %s or %s".formatted(tempBase, xpipeHome));
            }

            tempBase = xpipeHome;
            xpipeTemp = FileNames.join(tempBase, "temp");
        }

        var execTemp = FileNames.join(xpipeTemp, "exec");

        // Create and set all access permissions if not existent
        if (!d.directoryExists(proc, xpipeTemp).executeAndCheck()) {
            d.prepareTempDirectory(proc, xpipeTemp).execute();
        }

        // Check permissions for xpipe directory
        // If they don't match, delete it. We can do this as we are guaranteed to have all permissions in the parent directory
        else if (!checkDirectoryPermissions(proc, xpipeTemp)) {
            d.deleteFile(proc, xpipeTemp).execute();
            d.prepareTempDirectory(proc, xpipeTemp).execute();
        }

        // Create and set all access permissions if not existent
        if (!d.directoryExists(proc, execTemp).executeAndCheck()) {
            d.prepareTempDirectory(proc, execTemp).execute();
        }

        // Clear directory if it exists and is definitely not in use or the permissions do not match
        else if (!usedSystems.contains(proc.getSystemId()) || !checkDirectoryPermissions(proc, execTemp)) {
            d.deleteFile(proc, execTemp).execute();
            d.prepareTempDirectory(proc, execTemp).execute();
        }

        usedSystems.add(proc.getSystemId());
        return execTemp;
    }

    private static boolean checkDirectoryPermissions(ShellControl proc, String dir) throws Exception {
        if (proc.getOsType().equals(OsType.WINDOWS)) {
            return true;
        }

        var d = proc.getShellDialect();
        return proc.executeSimpleBooleanCommand("test -r %s && test -w %s && test -x %s"
                .formatted(d.fileArgument(dir), d.fileArgument(dir), d.fileArgument(dir)));
    }

    public static synchronized void occupyXPipeTempDirectory(ShellControl proc) {
        usedSystems.add(proc.getSystemId());
    }

    public static String getSubDirectory(ShellControl proc, String... sub) throws Exception {
        var base = proc.getSubTemporaryDirectory();
        var arr = Stream.concat(Stream.of(base), Arrays.stream(sub))
                .toArray(String[]::new);
        var dir = FileNames.join(arr);

        // We assume that this directory does not exist yet and therefore don't perform any checks
        proc.getShellDialect().prepareTempDirectory(proc, dir).execute();

        return dir;
    }
}
