package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionToggleExchange extends BeaconInterface<ConnectionToggleExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/toggle";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;

        boolean state;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
