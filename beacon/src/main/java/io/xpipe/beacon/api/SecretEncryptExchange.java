package io.xpipe.beacon.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.beacon.BeaconInterface;
import io.xpipe.core.util.SecretValue;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class SecretEncryptExchange extends BeaconInterface<SecretEncryptExchange.Request> {

    @Override
    public String getPath() {
        return "/secret/encrypt";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String value;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        JsonNode encrypted;
    }
}
