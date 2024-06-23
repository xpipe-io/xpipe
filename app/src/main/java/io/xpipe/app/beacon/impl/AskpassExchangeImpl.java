package io.xpipe.app.beacon.impl;

import io.xpipe.app.util.AskpassAlert;
import io.xpipe.app.util.SecretManager;
import io.xpipe.app.util.SecretQueryState;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.AskpassExchange;

import com.sun.net.httpserver.HttpExchange;

public class AskpassExchangeImpl extends AskpassExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        if (msg.getRequest() == null) {
            var r = AskpassAlert.queryRaw(msg.getPrompt(), null);
            return Response.builder().value(r.getSecret()).build();
        }

        var found = msg.getSecretId() != null
                ? SecretManager.getProgress(msg.getRequest(), msg.getSecretId())
                : SecretManager.getProgress(msg.getRequest());
        if (found.isEmpty()) {
            throw new BeaconClientException("Unknown askpass request");
        }

        var p = found.get();
        var secret = p.process(msg.getPrompt());
        if (p.getState() != SecretQueryState.NORMAL) {
            throw new BeaconClientException(SecretQueryState.toErrorMessage(p.getState()));
        }
        return Response.builder().value(secret.inPlace()).build();
    }
}
