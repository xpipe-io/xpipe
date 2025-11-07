package io.xpipe.app.prefs;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;

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

        raw = raw.strip();
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
            args = raw.substring(end + 1).strip();
        } else {
            exec = split.getFirst();
            args = split.stream().skip(1).collect(Collectors.joining(" "));
        }

        startAsync(CommandBuilder.of().addFile(exec).add(args));
    }

    public static void startAsync(CommandBuilder b) throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var base = b.buildBaseParts(sc);
            var exec = base.getFirst();
            if (exec.startsWith("\"") && exec.endsWith("\"")) {
                exec = exec.substring(1, exec.length() - 1);
            } else if (exec.startsWith("'") && exec.endsWith("'")) {
                exec = exec.substring(1, exec.length() - 1);
            }
            CommandSupport.isInPathOrThrow(sc, exec);

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
