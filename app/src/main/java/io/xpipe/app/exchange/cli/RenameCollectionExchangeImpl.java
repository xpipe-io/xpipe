package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.RenameCollectionExchange;

public class RenameCollectionExchangeImpl extends RenameCollectionExchange
        implements MessageExchangeImpl<RenameCollectionExchange.Request, RenameCollectionExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var col = getCollection(msg.getCollectionName());
        col.setName(msg.getNewName());
        return Response.builder().build();
    }
}
