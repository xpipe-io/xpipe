package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ShellExecExchange extends BeaconInterface<ShellExecExchange.Request> {

    @Override
    public String getPath() {
        return "/shell/exec";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;
        @NonNull
        String command;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        long exitCode;
        @NonNull
        String stdout;
        @NonNull
        String stderr;
    }
}
