package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellControl;

import java.io.IOException;
import java.util.List;

public class ApplicationHelper {

    public static void executeLocalApplication(String s) throws Exception {
        var args = ShellDialects.getPlatformDefault().executeCommandListWithShell(s);
        TrackEvent.withDebug("proc", "Executing local application")
                .tag("command", s)
                .handle();
        try (var c = LocalStore.getShell().command(s).start()) {
            c.discardOrThrow();
        }
    }

    public static void executeLocalApplication(List<String> s) throws Exception {
        TrackEvent.withDebug("proc", "Executing local application")
                .elements(s)
                .handle();
        try (var c = LocalStore.getShell().command(s).start()) {
            c.discardOrThrow();
        }
    }

    public static boolean isInPath(ShellControl processControl, String executable) throws Exception {
        return processControl.executeBooleanSimpleCommand(
                processControl.getShellDialect().getWhichCommand(executable));
    }

    public static void checkSupport(ShellControl processControl, String executable, String displayName)
            throws Exception {
        if (!isInPath(processControl, executable)) {
            throw new IOException(displayName + " executable " + executable + " not found in PATH");
        }
    }
}
