package io.xpipe.app.process;

import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class ProcessOutputException extends Exception {

    private final String command;
    private final long exitCode;
    private String output;
    private final String prefix;
    private final String suffix;

    private ProcessOutputException(
            String command, long exitCode, String output, String prefix, String suffix, Exception cause) {
        super(cause);
        this.exitCode = exitCode;
        this.output = output;
        this.prefix = prefix;
        this.suffix = suffix;
        this.command = command;
    }

    public void replaceOutput(String newOutput) {
        this.output = newOutput;
        if (getCause() instanceof ProcessOutputException p) {
            p.replaceOutput(newOutput);
        }
    }

    @Override
    public String getMessage() {
        if (prefix == null && suffix == null && "".equals(output)) {
            return null;
        }

        var messagePrefix = prefix != null ? prefix + "\n\n" : "";
        var messageSuffix = suffix != null ? "\n\n" + suffix : "";
        var message = messagePrefix + output + messageSuffix;
        return message;
    }

    public static ProcessOutputException withPrefix(String customPrefix, ProcessOutputException ex) {
        var joined = customPrefix + (ex.prefix != null ? "\n" + ex.prefix : "");
        return new ProcessOutputException(ex.getCommand(), ex.getExitCode(), ex.getOutput(), joined, null, ex);
    }

    public static ProcessOutputException withSuffix(String customSuffix, ProcessOutputException ex) {
        var joined = (ex.suffix != null ? ex.suffix + "\n" : "") + customSuffix;
        return new ProcessOutputException(ex.getCommand(), ex.getExitCode(), ex.getOutput(), null, joined, ex);
    }

    public static ProcessOutputException of(long exitCode, String... messages) {
        return of(null, exitCode, messages);
    }

    private static String formatMessage(String command, long exitCode, boolean includeCommand) {
        var start = command != null && includeCommand ? "Command\n" + command + "\n" : "Process ";
        var center = command != null && includeCommand ? "command\n" + command + "\n" : "process ";
        var message =
                switch ((int) exitCode) {
                    case CommandControl.START_FAILED_EXIT_CODE ->
                        start + "did not start up properly and had to be killed";
                    case CommandControl.EXIT_TIMEOUT_EXIT_CODE -> "Wait for exit of " + center + "timed out";
                    case CommandControl.UNASSIGNED_EXIT_CODE ->
                        start + "exited with unknown state. Did an external process interfere?";
                    case CommandControl.INTERNAL_ERROR_EXIT_CODE -> start + "execution failed";
                    case CommandControl.ELEVATION_FAILED_EXIT_CODE -> start + "elevation failed";
                    default -> start + "failed with exit code " + exitCode;
                };
        return message;
    }

    public String getDetailedMessage() {
        return formatMessage(command, exitCode, true);
    }

    public static ProcessOutputException of(String command, long exitCode, String... messages) {
        var combinedError = Arrays.stream(messages)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.strip())
                .collect(Collectors.joining("\n\n"))
                .replaceAll("\r\n", "\n");
        var message = formatMessage(command, exitCode, false);
        return new ProcessOutputException(command, exitCode, combinedError, message, null, null);
    }

    public boolean isIrregularExit() {
        return exitCode == CommandControl.EXIT_TIMEOUT_EXIT_CODE
                || exitCode == CommandControl.START_FAILED_EXIT_CODE
                || exitCode == CommandControl.UNASSIGNED_EXIT_CODE
                || exitCode == CommandControl.INTERNAL_ERROR_EXIT_CODE
                || exitCode == CommandControl.ELEVATION_FAILED_EXIT_CODE;
    }
}
