package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellControl;

import java.io.IOException;
import java.util.function.Function;

public class ApplicationHelper {

    public static void executeLocalApplication(Function<ShellControl, String> s, boolean detach) throws Exception {
        TrackEvent.withDebug("proc", "Executing local application")
                .tag("command", s)
                .handle();

        try (var sc = LocalStore.getShell().start()) {
            var cmd = detach ? ScriptHelper.createDetachCommand(sc, s.apply(sc)) : s.apply(sc);
            try (var c = sc.command(cmd).start()) {
                c.discardOrThrow();
            }
        }
    }

    public static boolean isInPath(ShellControl processControl, String executable) throws Exception {
        return processControl.executeSimpleBooleanCommand(
                processControl.getShellDialect().getWhichCommand(executable));
    }

    public static void checkSupport(
            ShellControl processControl, String executable, String displayName, String connectionName)
            throws Exception {
        if (!isInPath(processControl, executable)) {
            throw new IOException(displayName + " executable " + executable + " not found in PATH"
                    + (connectionName != null ? " on system " + connectionName : ""));
        }
    }
}
