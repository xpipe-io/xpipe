package io.xpipe.app.beacon.api;

import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.secret.DataStorageAccessHandler;
import io.xpipe.app.secret.EncryptionPrincipal;
import io.xpipe.app.secret.InPlaceSecretValue;
import io.xpipe.app.secret.MultiPrincipalSecret;
import io.xpipe.app.util.UuidHelper;

import com.sun.net.httpserver.HttpExchange;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SecretEncryptExchange extends BeaconInterface<SecretEncryptExchange.Request> {

    @Override
    public String getPath() {
        return "/secret/encrypt";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        Set<EncryptionPrincipal> resolvedPrincipals = new LinkedHashSet<>();
        if (msg.getPrincipals() != null) {
            EncryptionPrincipal p;
            for (String pr : msg.getPrincipals()) {
                var byName = DataStorageAccessHandler.getInstance().getAllEncryptionPrincipals().stream()
                        .filter(encryptionPrincipal ->
                                encryptionPrincipal.getName().equals(pr))
                        .findFirst();
                if (byName.isPresent()) {
                    p = byName.get();
                } else {
                    var uuid = UuidHelper.parse(pr);
                    if (uuid.isPresent()) {
                        var principal = DataStorageAccessHandler.getInstance().getEncryptionPrincipal(uuid.get());
                        p = principal.orElse(null);
                    } else {
                        p = null;
                    }
                }

                if (p == null) {
                    throw new BeaconClientException("Unknown principal " + pr);
                }

                if (!p.isAccessible()) {
                    throw new BeaconClientException("Principal " + p.getName() + " is not accessible");
                }

                resolvedPrincipals.add(p);
            }
        }

        if (resolvedPrincipals.isEmpty()) {
            resolvedPrincipals.add(DataStorageAccessHandler.getInstance().getEncryptAllPrincipal());
        }

        var secret = MultiPrincipalSecret.of(InPlaceSecretValue.of(msg.getValue()), resolvedPrincipals);
        return Response.builder().encrypted(secret.serialize()).build();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        String value;

        List<String> principals;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        JsonNode encrypted;
    }
}
