package io.xpipe.beacon.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.beacon.BeaconInterface;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class SecretDecryptExchange extends BeaconInterface<SecretDecryptExchange.Request> {

    @Override
    public String getPath() {
        return "/secret/decrypt";
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        JsonNode encrypted;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        String decrypted;
    }
}
