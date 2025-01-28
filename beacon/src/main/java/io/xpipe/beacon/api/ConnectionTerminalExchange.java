package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import io.xpipe.core.store.FilePath;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionTerminalExchange extends BeaconInterface<ConnectionTerminalExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/terminal";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;

        FilePath directory;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
