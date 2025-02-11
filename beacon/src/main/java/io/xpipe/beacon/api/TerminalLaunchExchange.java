package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.UUID;

public class TerminalLaunchExchange extends BeaconInterface<TerminalLaunchExchange.Request> {

    @Override
    public String getPath() {
        return "/terminal/launch";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID request;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        Path targetFile;
    }
}
