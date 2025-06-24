package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.beacon.api.SecretEncryptExchange;
import io.xpipe.core.util.InPlaceSecretValue;

import com.sun.net.httpserver.HttpExchange;

public class SecretEncryptExchangeImpl extends SecretEncryptExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var secret = DataStorageSecret.ofCurrentSecret(InPlaceSecretValue.of(msg.getValue()));
        return Response.builder().encrypted(secret.serialize(true)).build();
    }
}
