package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.process.ShellProcessControl;

import java.io.IOException;
import java.nio.file.Path;

public class XPipeTempDirectory {

    public static Path getLocal() {
        if (OsType.getLocal().equals(OsType.WINDOWS)) {
            return Path.of(System.getenv("TEMP")).resolve("xpipe");
        } else if (OsType.getLocal().equals(OsType.LINUX)) {
            return Path.of("/tmp/xpipe");
        } else {
            return Path.of(System.getenv("TMPDIR"), "xpipe");
        }
    }

    public static String get(ShellProcessControl proc) throws Exception {
        var base = proc.getOsType().getTempDirectory(proc);
        var dir = FileNames.join(base, "xpipe");

        if (!proc.executeBooleanSimpleCommand(proc.getShellDialect().getFileExistsCommand(dir))) {
            proc.executeSimpleCommand(
                    proc.getShellDialect().flatten(proc.getShellDialect().getMkdirsCommand(dir)),
                    "Unable to access or create temporary directory " + dir);

            if (proc.getOsType().equals(OsType.LINUX) || proc.getOsType().equals(OsType.MACOS)) {
                proc.executeSimpleCommand("chmod -f 777 \"" + dir + "\"");
            }
        }

        return dir;
    }

    public static void clear(ShellProcessControl proc) throws Exception {
        var dir = get(proc);
        if (!proc.executeBooleanSimpleCommand(proc.getShellDialect().getFileDeleteCommand(dir))) {
            throw new IOException("Unable to delete temporary directory " + dir);
        }
    }
}
