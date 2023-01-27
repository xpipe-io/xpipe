package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.RemoveEntryExchange;

public class RemoveEntryExchangeImpl extends RemoveEntryExchange
        implements MessageExchangeImpl<RemoveEntryExchange.Request, RemoveEntryExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var e = getSourceEntry(msg.getRef(), null, true);
        var id = DataStorage.get().getId(e);
        DataStorage.get().deleteEntry(e);
        return Response.builder().id(id).build();
    }
}
