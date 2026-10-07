package io.xpipe.app.prefs;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternalApplicationHelper {

    public static String replaceVariableArgument(String format, String variable, String value, boolean quoteLiteral) {
        // Support for legacy variables that were not upper case
        variable = variable.toUpperCase(Locale.ROOT);
        format = format.replace("$" + variable.toLowerCase(Locale.ROOT), "$" + variable.toUpperCase(Locale.ROOT));
        var substitute = quoteLiteral ? LocalShell.getDialect().literalArgument(value) : value;
        // Check if the variable is already quoted
        var quotedFormatString = format.contains("\"$" + variable + "\"");
        return format.replace(quotedFormatString ? ("\"$" + variable + "\"") : ("$" + variable), substitute);
    }

    public static void startAsync(String raw) throws Exception {
        if (raw == null) {
            return;
        }

        raw = raw.strip();
        if (raw.isEmpty()) {
            return;
        }

        var split = Arrays.asList(raw.split("\\s+"));

        String exec;
        String args;
        // The executable can be quoted with either double or single quotes
        if (raw.startsWith("\"") || raw.startsWith("'")) {
            var quote = raw.charAt(0);
            var end = raw.indexOf(quote, 1);
            if (end == -1) {
                return;
            }
            exec = raw.substring(1, end);
            args = raw.substring(end + 1).strip();
        } else {
            exec = split.getFirst();
            // Keep the original spacing, as it might be part of quoted arguments
            args = raw.substring(exec.length()).strip();
        }

        // On Windows, the arguments are passed as-is to the program, which only understands double quotes
        if (OsType.ofLocal() == OsType.WINDOWS) {
            args = convertSingleQuotedArguments(args);
        }

        startAsync(CommandBuilder.of().addFile(exec).addIf(!args.isEmpty(), args));
    }

    private static final Pattern SINGLE_QUOTED_ARGUMENT = Pattern.compile("'((?:[^']|'')*)'");

    private static String convertSingleQuotedArguments(String args) {
        return SINGLE_QUOTED_ARGUMENT
                .matcher(args)
                .replaceAll(m -> Matcher.quoteReplacement("\"" + m.group(1).replace("''", "'") + "\""));
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

            // Some commands might be joined together without splitting into multiple elements
            // e.g. user-provided commands
            if (!exec.contains(" ")) {
                var execFile = FilePath.of(exec);
                if (execFile.isAbsolute()) {
                    if (!sc.view().fileExists(execFile)) {
                        throw ErrorEventFactory.expected(new IOException("Executable " + execFile + " does not exist"));
                    }
                } else {
                    CommandSupport.isInPathOrThrow(sc, exec);
                }
            }

            var cmd = sc.getShellDialect().launchAsync(b, true);
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
