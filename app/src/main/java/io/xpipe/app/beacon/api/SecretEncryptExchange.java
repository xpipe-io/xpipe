package io.xpipe.app.beacon.api;

import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.secret.EncryptionPrincipal;
import io.xpipe.app.secret.InPlaceSecretValue;
import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.app.util.UuidHelper;

import com.sun.net.httpserver.HttpExchange;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.JsonNode;

import java.util.Set;

public class SecretEncryptExchange extends BeaconInterface<SecretEncryptExchange.Request> {

    @Override
    public String getPath() {
        return "/secret/encrypt";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        EncryptionPrincipal p;
        if (msg.getPrincipal() != null) {
            var byName = DataStorageAccessHandler.getInstance().getAllEncryptionPrincipals().stream()
                    .filter(encryptionPrincipal -> encryptionPrincipal.getName().equals(msg.getPrincipal()))
                    .findFirst();
            if (byName.isPresent()) {
                p = byName.get();
            } else {
                var uuid = UuidHelper.parse(msg.getPrincipal());
                if (uuid.isPresent()) {
                    var principal = DataStorageAccessHandler.getInstance().getEncryptionPrincipal(uuid.get());
                    p = principal.orElse(null);
                } else {
                    p = null;
                }
            }

            if (p == null) {
                throw new BeaconClientException("Unknown principal " + msg.getPrincipal());
            }

            if (!p.isAccessible()) {
                throw new BeaconClientException("Principal " + p.getName() + " is not accessible");
            }
        } else {
            p = DataStorageAccessHandler.getInstance().getEncryptAllPrincipal();
        }

        var secret = DataStorageSecret.of(InPlaceSecretValue.of(msg.getValue()), Set.of(p));
        return Response.builder().encrypted(secret.serialize()).build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String value;

        String principal;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        JsonNode encrypted;
    }
}
