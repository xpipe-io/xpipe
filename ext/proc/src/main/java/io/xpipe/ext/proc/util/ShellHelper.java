package io.xpipe.ext.proc.util;

import io.xpipe.core.process.*;
import io.xpipe.core.store.ShellStore;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static io.xpipe.core.process.OsType.*;
import static io.xpipe.core.process.ShellTypes.*;

public class ShellHelper {

    public static final int MAX_LINES = 100;
    private static final String DETECTOR_STRING = "-NoEnumerate \"shell($SHELL) name($0)\"";
    private static final String DETECTOR_COMMAND = "echo " + DETECTOR_STRING;

    public static String censor(String command,  boolean sensitive   ) {
        if (command == null) {
            return command;
        }

        if (!sensitive) {
            return command;
        }

        return "*".repeat(command.length());
    }

    public static OsType determineOsType(ShellProcessControl pc) throws Exception {
        try (CommandProcessControl c = pc.command(pc.getShellType().getFileExistsCommand("C:\\pagefile.sys"))
                .start()) {
            if (c.discardAndCheckExit()) {
                return WINDOWS;
            }
        }

        var uname = pc.executeStringSimpleCommand("uname");
        if (uname.equalsIgnoreCase("Darwin")) {
            return MAC;
        }

        return LINUX;
    }

    private static void normalizeShellState(ShellProcessControl proc, Charset parentCharset, String exitLine)
            throws Exception {
        var lastLine = "";
        proc.writeLine("");
        while (true) {
            var newline = ShellReader.readLineUntilTimeout(StandardCharsets.US_ASCII, proc.getStdout(), 1000);

            // This check is probably problematic in case you are using a shell with an empty shell prompt text
            if (newline.isEmpty()) {
                return;
            }

            if (exitLine != null && newline.getContent().contains(exitLine)) {
                throwUnsuccessfulException(proc, parentCharset, exitLine);
            }

            //
            if (newline.isHasSeenNewLine() || newline.getContent().equals(lastLine)) {
                return;
            }

            lastLine = newline.getContent();
            proc.writeLine("");
        }
    }

    private static void throwUnsuccessfulException(ShellProcessControl proc, Charset parentCharset, String exitLine)
            throws IOException {
        TrackEvent.error("Shell opener command was unsuccessful");
        var err = ShellReader.readUntilOccurrence(proc.getStderr(), parentCharset, exitLine, null, true)
                .strip();
        throw new IOException("Shell opener command failed" + (err.isEmpty() ? "" : ": " + err));
    }

    public static ShellType determineType(
            ShellProcessControl proc, Charset parentCharset, String cmdLine, String exitLine, int startTimeout)
            throws Exception {
        // This command will produce different outputs for sh, cmd, and powershell
        proc.writeLine(DETECTOR_COMMAND);

        var counter = 0;
        var attemptedNormalization = false;
        while (true) {
            var r = ShellReader.readLineUntilTimeout(StandardCharsets.US_ASCII, proc.getStdout(), startTimeout);

            // The shell is completely frozen, which is bad
            if (r.isEmpty()) {
                TrackEvent.error("Shell opener command timed out");
                var err = ShellReader.readAllUntilTimeout(parentCharset, proc.getStderr(), startTimeout)
                        .replace(exitLine != null ? exitLine : "", "")
                        .strip();
                throw new IOException("Shell opener command timed out" + (err.isEmpty() ? "" : ": " + err));
            }

            // The shell is working fine but decided to not send new lines.
            // We can recover from this inconsistent state though!
            if (!r.getContent().isEmpty() && !r.isHasSeenNewLine()) {
                if (attemptedNormalization) {
                    throw new IOException("Shell opener command could not be normalized");
                }

                TrackEvent.withWarn("proc", "Shell did not send complete line. Trying to recover ...")
                        .tag("sent", r.getContent())
                        .handle();
                normalizeShellState(proc, parentCharset, exitLine);
                attemptedNormalization = true;
                proc.writeLine(DETECTOR_COMMAND);
                continue;
            }

            var line = r.getContent();

            // This indicates that the shell session has ended
            // and a shell opener command therefore failed
            if (cmdLine != null && !line.contains(cmdLine) && exitLine != null && line.contains(exitLine)) {
                throwUnsuccessfulException(proc, parentCharset, exitLine);
            }

            // We know that it must be a Windows shell as only these echo the input.
            // However, we must read an additional line to determine which kind of Windows shell it is
            if (line.contains(DETECTOR_COMMAND)) {
                break;
            }

            // We know that it is a posix shell
            if (line.contains("-NoEnumerate")) {
                for (ShellType t : ShellTypes.getAllShellTypes()) {
                    if (line.contains("name(" + t.getName() + ")")) {
                        return t;
                    }
                }

                for (ShellType t : ShellTypes.getAllShellTypes()) {
                    if (line.contains("shell(" + t.getExecutable() + ")")) {
                        return t;
                    }
                }

                throw new IllegalStateException("Unable to determine POSIX shell: " + line);
            }

            if (counter > MAX_LINES) {
                throw new IOException("Shell opener command is stuck: " + line);
            }
            counter++;
        }

        var o = ShellReader.readLine(proc.getStdout(), StandardCharsets.US_ASCII);
        if (o.equals(DETECTOR_STRING)) {
            ShellReader.readLine(proc.getStdout(), StandardCharsets.US_ASCII);
            return CMD;
        } else {
            return POWERSHELL;
        }
    }

    public static List<ShellType> queryAvailableTypes(ShellStore store) throws Exception {
        try (var proc = store.create().start()) {
            return Arrays.stream(getAllShellTypes())
                    .filter(shellType -> {
                        try {
                            return proc.executeBooleanSimpleCommand(
                                    proc.getShellType().getWhichCommand(shellType.getName()));
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).omit().handle();
                            return false;
                        }
                    })
                    .toList();
        }
    }
}
