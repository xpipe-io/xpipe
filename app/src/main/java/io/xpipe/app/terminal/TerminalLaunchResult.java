package io.xpipe.app.terminal;

import lombok.Value;

import java.nio.file.Path;

public interface TerminalLaunchResult {

    @Value
    public static class ResultSuccess implements TerminalLaunchResult {
        Path targetScript;
    }

    @Value
    public static class ResultFailure implements TerminalLaunchResult {
        Throwable throwable;
    }
}
