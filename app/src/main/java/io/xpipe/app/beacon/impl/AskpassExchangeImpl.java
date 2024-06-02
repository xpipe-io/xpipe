package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.util.SecretManager;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.BeaconServerException;
import io.xpipe.beacon.api.AskpassExchange;

import java.io.IOException;

public class AskpassExchangeImpl extends AskpassExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws IOException, BeaconClientException, BeaconServerException {
        var found = msg.getSecretId() != null
                ? SecretManager.getProgress(msg.getRequest(), msg.getSecretId())
                : SecretManager.getProgress(msg.getRequest());
        if (found.isEmpty()) {
            return Response.builder().build();
        }

        var p = found.get();
        var secret = p.process(msg.getPrompt());
        return Response.builder()
                .value(secret != null ? secret.inPlace() : null)
                .build();
    }
}
