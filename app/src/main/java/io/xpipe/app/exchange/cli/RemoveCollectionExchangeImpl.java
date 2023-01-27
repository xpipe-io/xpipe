package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.RemoveCollectionExchange;

public class RemoveCollectionExchangeImpl extends RemoveCollectionExchange
        implements MessageExchangeImpl<RemoveCollectionExchange.Request, RemoveCollectionExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var col = getCollection(msg.getCollectionName());
        DataStorage.get().deleteCollection(col);
        return Response.builder().build();
    }
}
