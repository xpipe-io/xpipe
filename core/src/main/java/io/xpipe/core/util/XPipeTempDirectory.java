package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.ShellStore;

import java.io.IOException;
import java.nio.file.Path;

public class XPipeTempDirectory {

    public static Path getLocal() throws Exception {
        try (var pc = ShellStore.local().create().start()) {
            return Path.of(get(pc));
        }
    }

    public static String get(ShellProcessControl proc) throws Exception {
        var base = proc.getOsType().getTempDirectory(proc);
        var dir = FileNames.join(base, "xpipe");
        if (!proc.executeBooleanSimpleCommand(proc.getShellType().flatten(proc.getShellType().createMkdirsCommand(dir))) ){
            throw new IOException("Unable to access or create temporary directory " + dir);
        }

        return dir;
    }

    public static void clear(ShellProcessControl proc) throws Exception {
        var dir = get(proc);
        if (!proc.executeBooleanSimpleCommand(proc.getShellType().createFileDeleteCommand(dir)) ){
            throw new IOException("Unable to delete temporary directory " + dir);
        }
    }
}
