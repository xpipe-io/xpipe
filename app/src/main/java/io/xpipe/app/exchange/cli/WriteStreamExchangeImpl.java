package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.WriteStreamExchange;
import io.xpipe.core.store.StreamDataStore;

public class WriteStreamExchangeImpl extends WriteStreamExchange
        implements MessageExchangeImpl<WriteStreamExchange.Request, WriteStreamExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getStoreEntryByName(msg.getName(), false);
        StreamDataStore store = ds.getStore().asNeeded();
        try (var input = handler.receiveBody(); var output = store.openOutput()) {
            input.transferTo(output);
        }
        return WriteStreamExchange.Response.builder().build();
    }
}
