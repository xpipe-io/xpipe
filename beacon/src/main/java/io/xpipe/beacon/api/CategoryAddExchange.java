package io.xpipe.beacon.api;

import io.xpipe.beacon.BeaconInterface;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class CategoryAddExchange extends BeaconInterface<CategoryAddExchange.Request> {

    @Override
    public String getPath() {
        return "/category/add";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String name;

        @NonNull
        UUID parent;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID category;
    }
}
