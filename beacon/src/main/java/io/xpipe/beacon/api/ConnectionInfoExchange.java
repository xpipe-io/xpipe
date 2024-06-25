package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.StorePath;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

public class ConnectionInfoExchange extends BeaconInterface<ConnectionInfoExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/info";
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
    public static class Response {
        @NonNull
        UUID connection;

        @NonNull
        StorePath category;

        @NonNull
        StorePath name;

        @NonNull
        String type;

        @NonNull
        Object rawData;

        @NonNull
        Object usageCategory;

        @NonNull
        Instant lastUsed;

        @NonNull
        Instant lastModified;
    }
}
