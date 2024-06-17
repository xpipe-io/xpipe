package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.util.AskpassAlert;
import io.xpipe.app.util.SecretManager;
import io.xpipe.beacon.api.AskpassExchange;

public class AskpassExchangeImpl extends AskpassExchange {

    @Override
    public boolean requiresCompletedStartup() {
        return false;
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        if (msg.getRequest() == null) {
            var r = AskpassAlert.queryRaw(msg.getPrompt(), null);
            return Response.builder().value(r.getSecret()).build();
        }

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
