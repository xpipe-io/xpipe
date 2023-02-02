package io.xpipe.extension.util;

import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.process.ShellTypes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ApplicationHelper {

    public static void executeLocalApplication(String s) throws Exception {
        var args = ShellTypes.getPlatformDefault().executeCommandListWithShell(s);
        var p = new ProcessBuilder(args).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        var error = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        if (p.waitFor() != 0) {
            throw new IOException(error);
        }
    }

    public static boolean isInPath(ShellProcessControl processControl, String executable) throws Exception {
        return processControl.executeBooleanSimpleCommand(processControl.getShellType().getWhichCommand(executable));
    }

    public static void checkSupport(ShellProcessControl processControl, String executable, String displayName) throws Exception {
        if (!isInPath(processControl, executable)) {
            throw new IOException(displayName + " executable " + executable + " not found in PATH");
        }
    }
}
