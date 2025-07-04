package io.xpipe.beacon.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.beacon.BeaconInterface;

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
        JsonNode data;

        @NonNull
        Boolean validate;

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
