package io.xpipe.app.process;

import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class ProcessOutputException extends Exception {

    private final long exitCode;
    private String output;
    private final String prefix;
    private final String suffix;

    private ProcessOutputException(long exitCode, String output, String prefix, String suffix, Exception cause) {
        super(cause);
        this.exitCode = exitCode;
        this.output = output;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public void replaceOutput(String newOutput) {
        this.output = newOutput;
        if (getCause() instanceof ProcessOutputException p) {
            p.replaceOutput(newOutput);
        }
    }

    @Override
    public String getMessage() {
        var messagePrefix = prefix != null ? prefix + "\n\n" : "";
        var messageSuffix = suffix != null ? "\n\n" + suffix : "";
        var message = messagePrefix + output + messageSuffix;
        return message;
    }

    public static ProcessOutputException withPrefix(String customPrefix, ProcessOutputException ex) {
        var joined = customPrefix + (ex.prefix != null ? "\n" + ex.prefix : "");
        return new ProcessOutputException(ex.getExitCode(), ex.getOutput(), joined, null, ex);
    }

    public static ProcessOutputException withSuffix(String customSuffix, ProcessOutputException ex) {
        var joined = (ex.suffix != null ? ex.suffix + "\n" : "") + customSuffix;
        return new ProcessOutputException(ex.getExitCode(), ex.getOutput(), null, joined, ex);
    }

    public static ProcessOutputException of(long exitCode, String... messages) {
        var combinedError = Arrays.stream(messages)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.strip())
                .collect(Collectors.joining("\n\n"))
                .replaceAll("\r\n", "\n");
        var message =
                switch ((int) exitCode) {
                    case CommandControl.START_FAILED_EXIT_CODE ->
                        "Process did not start up properly and had to be killed";
                    case CommandControl.EXIT_TIMEOUT_EXIT_CODE -> "Wait for process exit timed out";
                    case CommandControl.UNASSIGNED_EXIT_CODE ->
                        "Process exited with unknown state. Did an external process interfere?";
                    case CommandControl.INTERNAL_ERROR_EXIT_CODE -> "Process execution failed";
                    case CommandControl.ELEVATION_FAILED_EXIT_CODE -> "Process elevation failed";
                    default -> "Process returned exit code " + exitCode;
                };
        return new ProcessOutputException(exitCode, combinedError, message, null, null);
    }

    public boolean isIrregularExit() {
        return exitCode == CommandControl.EXIT_TIMEOUT_EXIT_CODE
                || exitCode == CommandControl.START_FAILED_EXIT_CODE
                || exitCode == CommandControl.UNASSIGNED_EXIT_CODE
                || exitCode == CommandControl.INTERNAL_ERROR_EXIT_CODE
                || exitCode == CommandControl.ELEVATION_FAILED_EXIT_CODE;
    }
}
