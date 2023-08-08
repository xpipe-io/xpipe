package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.ShellControl;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Function;

public class ApplicationHelper {

    public static String replaceFileArgument(String format, String variable, String file) {
        // Support for legacy variables that were not upper case
        variable = variable.toUpperCase(Locale.ROOT);
        format = format.replace("$" + variable.toLowerCase(Locale.ROOT), "$" + variable.toUpperCase(Locale.ROOT));

        var fileString = file.contains(" ") ? "\"" + file + "\"" : file;
        // Check if the variable is already quoted
        return format.replace("\"$" + variable + "\"", fileString).replace("$" + variable, fileString);
    }

    public static void executeLocalApplication(Function<ShellControl, String> s, boolean detach) throws Exception {
        try (var sc = LocalStore.getShell().start()) {
            var cmd = detach ? ScriptHelper.createDetachCommand(sc, s.apply(sc)) : s.apply(sc);
            TrackEvent.withDebug("proc", "Executing local application")
                    .tag("command", cmd)
                    .handle();
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
