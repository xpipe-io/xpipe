package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionRefreshExchange extends BeaconInterface<ConnectionRefreshExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/refresh";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
