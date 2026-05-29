package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.util.InPlaceSecretValue;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

public class SecretEncryptExchange extends BeaconInterface<SecretEncryptExchange.Request> {

    @Override
    public String getPath() {
        return "/secret/encrypt";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var secret = DataStorageSecret.ofCurrentSecret(InPlaceSecretValue.of(msg.getValue()));
        return Response.builder().encrypted(secret.serialize(true)).build();
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
