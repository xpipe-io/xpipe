package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.cli.RenameStoreExchange;

public class RenameStoreExchangeImpl extends RenameStoreExchange
        implements MessageExchangeImpl<RenameStoreExchange.Request, RenameStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var s = DataStorage.get().getStore(msg.getStoreName(), true);
        DataStorage.get().renameStore(s, msg.getNewName());
        return Response.builder().build();
    }
}
