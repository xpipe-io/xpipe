package io.xpipe.app.prefs;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class ExternalApplicationHelper {

    public static String replaceVariableArgument(String format, String variable, String value) {
        // Support for legacy variables that were not upper case
        variable = variable.toUpperCase(Locale.ROOT);
        format = format.replace("$" + variable.toLowerCase(Locale.ROOT), "$" + variable.toUpperCase(Locale.ROOT));

        var fileString = value.contains(" ") ? "\"" + value + "\"" : value;
        // Check if the variable is already quoted
        return format.replace("\"$" + variable + "\"", fileString).replace("$" + variable, fileString);
    }

    public static void startAsync(String raw) throws Exception {
        if (raw == null) {
            return;
        }

        raw = raw.trim();
        var split = Arrays.asList(raw.split("\\s+"));
        if (split.size() == 0) {
            return;
        }

        String exec;
        String args;
        if (raw.startsWith("\"")) {
            var end = raw.substring(1).indexOf("\"");
            if (end == -1) {
                return;
            }
            end++;
            exec = raw.substring(1, end);
            args = raw.substring(end + 1).trim();
        } else {
            exec = split.getFirst();
            args = split.stream().skip(1).collect(Collectors.joining(" "));
        }

        startAsync(CommandBuilder.of().addFile(exec).add(args));
    }

    public static void startAsync(CommandBuilder b) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var cmd = sc.getShellDialect().launchAsnyc(b);
            TrackEvent.withDebug("Executing local application")
                    .tag("command", b.buildFull(sc))
                    .tag("adjusted", cmd.buildFull(sc))
                    .handle();
            try (var c = sc.command(cmd).start()) {
                c.discardOrThrow();
            }
        }
    }
}
