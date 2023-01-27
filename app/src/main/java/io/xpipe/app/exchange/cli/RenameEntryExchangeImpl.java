package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.RenameEntryExchange;

public class RenameEntryExchangeImpl extends RenameEntryExchange
        implements MessageExchangeImpl<RenameEntryExchange.Request, RenameEntryExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var e = getSourceEntry(msg.getRef(), null, true);
        DataStorage.get().deleteEntry(e);
        DataStorage.get()
                .add(
                        e,
                        DataStorage.get()
                                .getCollectionForName(msg.getNewId().getCollectionName())
                                .orElseThrow());
        e.setName(msg.getNewId().getEntryName());
        return Response.builder().build();
    }
}
