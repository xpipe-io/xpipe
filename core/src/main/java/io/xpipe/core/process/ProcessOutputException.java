package io.xpipe.core.process;

import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class ProcessOutputException extends Exception {

    public static ProcessOutputException withPrefix(String customPrefix, ProcessOutputException ex) {
        var messageSuffix = ex.getOutput() != null && !ex.getOutput().isBlank() ? ":\n" + ex.getOutput() : "";
        var message = customPrefix + messageSuffix;
        return new ProcessOutputException(message, ex.getExitCode(), ex.getOutput());
    }

    public static ProcessOutputException of(int exitCode, String... messages) {
        var combinedError = Arrays.stream(messages)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.strip())
                .collect(Collectors.joining("\n\n"))
                .replaceAll("\r\n", "\n");
        var hasMessage = !combinedError.isBlank();
        var errorSuffix = hasMessage ? ":\n" + combinedError : "";
        var message =
                switch (exitCode) {
                    case CommandControl
                            .START_FAILED_EXIT_CODE -> "Process did not start up properly and had to be killed"
                            + errorSuffix;
                    case CommandControl.EXIT_TIMEOUT_EXIT_CODE -> "Wait for process exit timed out" + errorSuffix;
                    case CommandControl.UNASSIGNED_EXIT_CODE -> "Process exited with unknown state. Did an external process interfere?" + errorSuffix;
                    default -> "Process returned exit code " + exitCode + errorSuffix;
                };
        return new ProcessOutputException(message, exitCode, combinedError);
    }

    private final int exitCode;
    private final String output;

    private ProcessOutputException(String message, int exitCode, String output) {
        super(message);
        this.exitCode = exitCode;
        this.output = output;
    }

    public boolean isTimeOut() {
        return exitCode == CommandControl.EXIT_TIMEOUT_EXIT_CODE;
    }

    public boolean isKill() {
        return exitCode == CommandControl.START_FAILED_EXIT_CODE;
    }
}
