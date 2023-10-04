package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.update.XPipeInstanceHelper;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.InstanceExchange;
import io.xpipe.core.store.LocalStore;

public class InstanceExchangeImpl extends InstanceExchange
        implements MessageExchangeImpl<InstanceExchange.Request, InstanceExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        return Response.builder()
                .instance(XPipeInstanceHelper.getInstance(new LocalStore()).orElseThrow())
                .build();
    }
}
