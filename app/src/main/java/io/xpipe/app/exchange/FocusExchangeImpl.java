package io.xpipe.app.exchange;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.FocusExchange;

public class FocusExchangeImpl extends FocusExchange
        implements MessageExchangeImpl<FocusExchange.Request, FocusExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        OperationMode.switchUp(OperationMode.map(msg.getMode()));
        return Response.builder().build();
    }
}
