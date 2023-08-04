package io.xpipe.app.exchange;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.launcher.LauncherInput;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.OpenExchange;

public class OpenExchangeImpl extends OpenExchange
        implements MessageExchangeImpl<OpenExchange.Request, OpenExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        if (msg.getArguments().isEmpty()) {
            OperationMode.switchToAsync(OperationMode.GUI);
        }

        LauncherInput.handle(msg.getArguments());
        return Response.builder().build();
    }
}
