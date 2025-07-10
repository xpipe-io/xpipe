package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.StorePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        List<UUID> connections;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        List<@NonNull InfoResponse> infos;
    }

    @Jacksonized
    @Builder
    @Value
    public static class InfoResponse {
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

        @NonNull
        Object state;

        @NonNull
        Map<String, Object> cache;
    }
}
