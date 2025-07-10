package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

public class CategoryQueryExchange extends BeaconInterface<CategoryQueryExchange.Request> {

    @Override
    public String getPath() {
        return "/category/query";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String filter;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        List<@NonNull UUID> found;
    }
}
