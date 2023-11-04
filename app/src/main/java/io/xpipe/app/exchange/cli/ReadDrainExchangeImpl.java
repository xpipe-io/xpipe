package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.ReadDrainExchange;

public class ReadDrainExchangeImpl extends ReadDrainExchange implements MessageExchangeImpl<ReadDrainExchange.Request, ReadDrainExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        return ReadDrainExchange.Response.builder().build();
    }
}
