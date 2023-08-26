package io.xpipe.core.process;

import io.xpipe.core.store.MessageFormatter;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public class ProcessOutputException extends Exception {

    public static ProcessOutputException withPrefix(String customPrefix, ProcessOutputException ex) {
        var messageSuffix = ex.getRawOutput() != null && !ex.getRawOutput().isBlank() ? ":\n" + ex.getRawOutput() : "";
        var message = customPrefix + messageSuffix;
        return new ProcessOutputException(message, ex.getExitCode(), ex.getRawOutput());
    }

    public static ProcessOutputException ofCommand(CommandControl commandControl, String rawOutput) {
        var fmt = commandControl.getMessageFormatter();
        if (commandControl.getParent() != null && commandControl.getParent().getShellDialect() != null) {
            fmt = fmt.or(commandControl.getParent().getShellDialect().defaultCommandFormatter());
        }
        fmt = fmt.or(ExitCode.commandMessageFormatter());
        return new ProcessOutputException(fmt.format(commandControl, rawOutput), commandControl.getExitCode(), rawOutput);
    }

    public static ProcessOutputException ofShell(ShellControl shellControl, String rawOutput) {
        var fmt = shellControl.getMessageFormatter();
        if (shellControl.getShellDialect() != null) {
            fmt = fmt.or(shellControl.getShellDialect().defaultShellFormatter());
        }
        fmt = fmt.or((sc, message) -> "Shell startup failed:\n" + message);
        return new ProcessOutputException(fmt.format(shellControl, rawOutput), ExitCode.UNASSIGNED_EXIT_CODE, rawOutput);
    }

    public static ProcessOutputException ofFormatted(MessageFormatter formatter, String... messages) {
        var combinedError = toMessage(messages);
        return new ProcessOutputException(formatter.format(combinedError), ExitCode.UNASSIGNED_EXIT_CODE, combinedError);
    }

    public static ProcessOutputException ofShell(ShellControl shellControl, String... messages) {
        var combinedError = toMessage(messages);
        return ofShell(shellControl, combinedError);
    }

    public static ProcessOutputException ofCommand(CommandControl commandControl, String... messages) {
        var combinedError = toMessage(messages);
        return ofCommand(commandControl, combinedError);
    }

    public static String toMessage(String... messages) {
        var combinedError = Arrays.stream(messages)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.strip())
                .collect(Collectors.joining("\n\n"))
                .replaceAll("\r\n", "\n");
        return combinedError;
    }

    private final int exitCode;
    private final String rawOutput;
    private final String formattedMessage;

    private ProcessOutputException(String message, int exitCode, String rawOutput) {
        super(rawOutput);
        this.exitCode = exitCode;
        this.rawOutput = rawOutput;
        this.formattedMessage = message;
    }

    public boolean isTimeOut() {
        return exitCode == ExitCode.EXIT_TIMEOUT_EXIT_CODE;
    }

    public boolean isKill() {
        return exitCode == ExitCode.START_FAILED_EXIT_CODE;
    }
}
