package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.DrainExchange;
import io.xpipe.core.store.ShellStore;

public class DrainExchangeImpl extends DrainExchange implements MessageExchangeImpl<DrainExchange.Request, DrainExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getStoreEntryById(msg.getSource(), false);

        if (!(ds.getStore() instanceof ShellStore)) {
            throw new ClientException("Can't open file system for connection");
        }

        handler.postResponse(() -> {
            ShellStore store = ds.getStore().asNeeded();
            try (var fs = store.createFileSystem(); var output = handler.sendBody(); var inputStream = fs.openInput(msg.getPath())) {
                inputStream.transferTo(output);
            }
        });
        return Response.builder().build();
    }
}
