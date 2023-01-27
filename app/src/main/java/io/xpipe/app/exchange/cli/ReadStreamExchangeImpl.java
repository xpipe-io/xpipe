package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.exchange.ReadStreamExchange;
import io.xpipe.core.store.StreamDataStore;

import java.io.InputStream;

public class ReadStreamExchangeImpl extends ReadStreamExchange
        implements MessageExchangeImpl<ReadStreamExchange.Request, ReadStreamExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getStoreEntryByName(msg.getName(), false);

        handler.postResponse(() -> {
            StreamDataStore store = ds.getStore().asNeeded();
            try (var output = handler.sendBody();
                    InputStream inputStream = store.openInput()) {
                inputStream.transferTo(output);
            }
        });
        return Response.builder().build();
    }
}
