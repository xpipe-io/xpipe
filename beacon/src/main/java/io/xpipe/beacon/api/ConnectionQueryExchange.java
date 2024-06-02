package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.StorePath;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

public class ConnectionQueryExchange extends BeaconInterface<ConnectionQueryExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/query";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String categoryFilter;
        @NonNull
        String connectionFilter;
        @NonNull
        String typeFilter;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        List<QueryResponse> found;
    }

    @Jacksonized
    @Builder
    @Value
    public static class QueryResponse {
        UUID uuid;
        StorePath category;
        StorePath connection;
        String type;
    }
}
