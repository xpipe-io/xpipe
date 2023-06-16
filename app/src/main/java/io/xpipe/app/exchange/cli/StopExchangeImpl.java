package io.xpipe.app.exchange.cli;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.StopExchange;

public class StopExchangeImpl extends StopExchange
        implements MessageExchangeImpl<StopExchange.Request, StopExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        handler.postResponse(() -> {
            ThreadHelper.runAsync(() -> {
                ThreadHelper.sleep(1000);
                OperationMode.close();
            });
        });
        return Response.builder().success(true).build();
    }
}
