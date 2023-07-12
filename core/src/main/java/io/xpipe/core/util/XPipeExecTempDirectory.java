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

    public static synchronized String initExecTempDirectory(ShellControl proc) throws Exception {
        var d = proc.getShellDialect();
        var home = proc.getOsType().getHomeDirectory(proc);

        // We assume that this exists now as the systemid should have been created in this
        var xpipeHome = FileNames.join(home, ".xpipe");
        var targetTemp = FileNames.join(xpipeHome, "temp");

        var systemTemp = proc.getOsType().getTempDirectory(proc);
        var legacyTemp = FileNames.join(systemTemp, "xpipe");
        var legacyExecTemp = FileNames.join(legacyTemp, "exec");

        // Always delete legacy directory and do not care whether it partially fails
        d.deleteFile(proc, legacyExecTemp).executeAndCheck();

        // Check permissions for home temp directory
        // If this is somehow messed up, we can still default back to the system directory
        if (!checkDirectoryPermissions(proc, targetTemp)) {
            if (!d.directoryExists(proc, systemTemp).executeAndCheck() || !checkDirectoryPermissions(proc, systemTemp)) {
                throw new IOException("No permissions to create scripts in either %s or %s".formatted(systemTemp, targetTemp));
            }

            targetTemp = systemTemp;
        } else {
            // Create and set all access permissions if not existent
            if (!d.directoryExists(proc, targetTemp).executeAndCheck()) {
                d.prepareUserTempDirectory(proc, targetTemp).execute();
            } else if (!usedSystems.contains(proc.getSystemId())) {
                // Try to clear directory and do not care about errors
                d.deleteFile(proc, targetTemp).executeAndCheck();
                d.prepareUserTempDirectory(proc, targetTemp).executeAndCheck();
            } else {
                // Still attempt to properly set permissions every time
                d.prepareUserTempDirectory(proc, targetTemp).executeAndCheck();
            }

        }

        usedSystems.add(proc.getSystemId());
        return targetTemp;
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
        proc.getShellDialect().prepareUserTempDirectory(proc, dir).execute();

        return dir;
    }
}
