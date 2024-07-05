package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

public class ConnectionRemoveExchange extends BeaconInterface<ConnectionRemoveExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/remove";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        List<UUID> connections;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
