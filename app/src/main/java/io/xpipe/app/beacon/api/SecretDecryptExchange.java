package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import com.fasterxml.jackson.databind.JsonNode;
import io.xpipe.app.storage.DataStorageSecret;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;

public class SecretDecryptExchange extends BeaconInterface<SecretDecryptExchange.Request> {

    @Override
    public String getPath() {
        return "/secret/decrypt";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException {
        var secret = DataStorageSecret.deserialize(msg.getEncrypted());
        if (secret == null) {
            throw new BeaconClientException("Unable to parse secret");
        }

        return Response.builder().decrypted(new String(secret.getSecret())).build();
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
