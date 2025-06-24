package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.StorePath;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CategoryInfoExchange extends BeaconInterface<CategoryInfoExchange.Request> {

    @Override
    public String getPath() {
        return "/category/info";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        List<UUID> categories;
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
        UUID category;

        @NonNull
        UUID parentCategory;

        @NonNull
        StorePath name;

        @NonNull
        Instant lastUsed;

        @NonNull
        Instant lastModified;

        @NonNull
        JsonNode config;
    }
}
