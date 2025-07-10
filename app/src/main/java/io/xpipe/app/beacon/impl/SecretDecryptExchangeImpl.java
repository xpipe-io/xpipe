package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorageSecret;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.SecretDecryptExchange;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class SecretDecryptExchangeImpl extends SecretDecryptExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException {
        var secret = DataStorageSecret.deserialize(msg.getEncrypted());
        if (secret == null) {
            throw new BeaconClientException("Unable to parse secret");
        }

        return Response.builder().decrypted(new String(secret.getSecret())).build();
    }
}
