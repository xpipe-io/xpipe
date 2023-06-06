package io.xpipe.core.util;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellControl;

import java.util.Arrays;
import java.util.stream.Stream;

public class XPipeTempDirectory {

    public static String getSystemTempDirectory(ShellControl proc) throws Exception {
        return proc.getOsType().getTempDirectory(proc);
    }

    public static String initXPipeTempDirectory(ShellControl proc) throws Exception {
        var base = proc.getOsType().getTempDirectory(proc);
        var arr = Stream.of(base, "xpipe").toArray(String[]::new);
        var dir = FileNames.join(arr);

        var existsCommand = proc.getShellDialect().createFileExistsCommand(proc, dir);
        if (existsCommand.executeAndCheck()) {
            proc.executeSimpleCommand(proc.getShellDialect().getFileDeleteCommand(dir));
        }

        proc.getShellDialect().prepareTempDirectory(proc, dir);

        return dir;
    }

    public static String getSubDirectory(ShellControl proc, String... sub) throws Exception {
        var base = proc.getOsType().getTempDirectory(proc);
        var arr = Stream.concat(Stream.of(base, "xpipe"), Arrays.stream(sub)).toArray(String[]::new);
        var dir = FileNames.join(arr);

        var existsCommand = proc.getShellDialect().createFileExistsCommand(proc, dir);
        if (!existsCommand.executeAndCheck()) {
            proc.getShellDialect().prepareTempDirectory(proc,dir).execute();
        }

        return dir;
    }
}
