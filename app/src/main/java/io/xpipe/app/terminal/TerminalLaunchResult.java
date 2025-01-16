package io.xpipe.app.terminal;

import lombok.Value;

import java.nio.file.Path;

public interface TerminalLaunchResult {

    @Value
    class ResultSuccess implements TerminalLaunchResult {
        Path targetScript;
    }

    @Value
    class ResultFailure implements TerminalLaunchResult {
        Throwable throwable;
    }
}
