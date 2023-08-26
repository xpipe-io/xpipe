package io.xpipe.core.process;

import io.xpipe.core.store.CommandMessageFormatter;

public class ExitCode {

    public static final int UNASSIGNED_EXIT_CODE = -1;
    public static final int EXIT_TIMEOUT_EXIT_CODE = -2;
    public static final int START_FAILED_EXIT_CODE = -3;
    public static final int INTERNAL_ERROR_EXIT_CODE = -4;

    public static CommandMessageFormatter commandMessageFormatter() {
        return (commandControl, message) -> {
            var hasMessage = !message.isBlank();
            var errorSuffix = hasMessage ? ":\n" + message : "";
            return switch (commandControl.getExitCode()) {
                        case ExitCode
                                .START_FAILED_EXIT_CODE -> "Process did not start up properly and had to be killed"
                                + errorSuffix;
                        case ExitCode.EXIT_TIMEOUT_EXIT_CODE -> "Wait for process exit timed out" + errorSuffix;
                        case ExitCode.UNASSIGNED_EXIT_CODE -> "Process exited with unknown state" + errorSuffix;
                        case ExitCode.INTERNAL_ERROR_EXIT_CODE -> "Process execution failed" + errorSuffix;
                        default -> "Process returned exit code " + commandControl.getExitCode() + errorSuffix;
                    };
        };
    }
}
