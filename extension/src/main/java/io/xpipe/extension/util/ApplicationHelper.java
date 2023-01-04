package io.xpipe.extension.util;

import io.xpipe.core.process.ShellProcessControl;

import java.io.IOException;

public class ApplicationHelper {

    public static boolean isInPath(ShellProcessControl processControl, String executable) throws Exception {
        return processControl.executeBooleanSimpleCommand(processControl.getShellType().getWhichCommand(executable));
    }

    public static void checkSupport(ShellProcessControl processControl, String executable, String displayName) throws Exception {
        if (!isInPath(processControl, executable)) {
            throw new IOException(displayName + " executable " + executable + " not found in PATH");
        }
    }
}
