package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.DataStore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionAddExchange extends BeaconInterface<ConnectionAddExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/add";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String name;

        @NonNull
        DataStore data;

        UUID category;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID connection;
    }
}
