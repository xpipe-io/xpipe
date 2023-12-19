package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.FailableSupplier;

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
        try (var sc = LocalShell.getShell().start()) {
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

    public static boolean isInPathSilent(ShellControl processControl, String executable) {
        try {
            return processControl.executeSimpleBooleanCommand(
                    processControl.getShellDialect().getWhichCommand(executable));
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public static void checkIsInPath(
            ShellControl processControl, String executable, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!isInPath(processControl, executable)) {
            throw ErrorEvent.unreportable(new IOException(displayName + " executable " + executable + " not found in PATH"
                    + (connection != null ? " on system " + connection.getName() : "")));
        }
    }

    public static void isSupported(FailableSupplier<Boolean> supplier, String displayName, DataStoreEntry connection)
            throws Exception {
        if (!supplier.get()) {
            throw ErrorEvent.unreportable(new IOException(displayName + " is not supported"
                                                                  + (connection != null ? " on system " + connection.getName() : "")));
        }
    }
}
