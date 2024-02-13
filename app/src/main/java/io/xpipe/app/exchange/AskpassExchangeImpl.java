package io.xpipe.app.exchange;

import io.xpipe.app.core.AppStyle;
import io.xpipe.app.core.AppTheme;
import io.xpipe.app.util.SecretManager;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.AskpassExchange;

public class AskpassExchangeImpl extends AskpassExchange
        implements MessageExchangeImpl<AskpassExchange.Request, AskpassExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        var found = msg.getSecretId() != null ? SecretManager.getProgress(msg.getRequest(), msg.getSecretId()) : SecretManager.getProgress(msg.getRequest());
        if (found.isEmpty()) {
            return Response.builder().build();
        }

        AppStyle.init();
        AppTheme.init();

        var p = found.get();
        var secret = p.process(msg.getPrompt());
        return Response.builder().value(secret != null ? secret.inPlace() : null).build();
    }
}
