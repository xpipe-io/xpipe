package io.xpipe.beacon.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.store.FilePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ActionExchange extends BeaconInterface<ActionExchange.Request> {

    @Override
    public String getPath() {
        return "/action";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        JsonNode action;

        boolean confirm;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
