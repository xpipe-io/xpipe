package io.xpipe.core.process;

import lombok.Getter;

@Getter
public class ProcessOutputException extends Exception {

    public static ProcessOutputException of(String customPrefix, ProcessOutputException ex) {
        var messageSuffix = ex.getOutput() != null && ! ex.getOutput().isBlank()?": " +  ex.getOutput() : "";
        var message = customPrefix + messageSuffix;
        return new ProcessOutputException(message, ex.getExitCode(), ex.getOutput());
    }

    public static ProcessOutputException of(int exitCode, String output) {
        var messageSuffix = output != null && !output.isBlank()?": " + output : "";
        var message = exitCode == -1 ? "Process timed out" + messageSuffix : "Process returned with exit code " + exitCode + messageSuffix;
        return new ProcessOutputException(message, exitCode, output);
    }

    private final int exitCode;
    private final String output;

    private  ProcessOutputException(String message, int exitCode, String output) {
        super(message);
        this.exitCode = exitCode;
        this.output = output;
    }

    public boolean isTimeOut() {
        return exitCode  == -1;
    }
}
