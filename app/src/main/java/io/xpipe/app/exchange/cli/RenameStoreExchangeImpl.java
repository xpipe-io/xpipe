package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.RenameStoreExchange;
import io.xpipe.core.store.DataStoreId;

public class RenameStoreExchangeImpl extends RenameStoreExchange
        implements MessageExchangeImpl<RenameStoreExchange.Request, RenameStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws ClientException {
        var s = getStoreEntryById(DataStoreId.fromString(msg.getStoreName()), true);
        s.setName(msg.getNewName());
        return Response.builder().build();
    }
}
