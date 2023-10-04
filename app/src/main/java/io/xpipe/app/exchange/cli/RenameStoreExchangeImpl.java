package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.RenameStoreExchange;

public class RenameStoreExchangeImpl extends RenameStoreExchange
        implements MessageExchangeImpl<RenameStoreExchange.Request, RenameStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) {
        var s = DataStorage.get().getStoreEntry(msg.getStoreName(), true);
        s.setName(msg.getNewName());
        return Response.builder().build();
    }
}
