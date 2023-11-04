package io.xpipe.app.exchange.cli;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.StatusExchange;

public class StatusExchangeImpl extends StatusExchange implements MessageExchangeImpl<StatusExchange.Request, StatusExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        String mode;
        if (OperationMode.get() == null) {
            mode = "none";
        } else {
            mode = OperationMode.get().getId();
        }

        return Response.builder().mode(mode).build();
    }
}
