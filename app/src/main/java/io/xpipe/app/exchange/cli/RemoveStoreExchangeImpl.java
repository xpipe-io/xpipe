package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.cli.RemoveStoreExchange;

public class RemoveStoreExchangeImpl extends RemoveStoreExchange
        implements MessageExchangeImpl<RemoveStoreExchange.Request, RemoveStoreExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var s = DataStorage.get().getStoreEntry(msg.getStoreName(), true);
        if (!s.getConfiguration().isDeletable()) {
            throw new ClientException("Store is not deletable");
        }

        DataStorage.get().deleteStoreEntry(s);
        return Response.builder().build();
    }
}
