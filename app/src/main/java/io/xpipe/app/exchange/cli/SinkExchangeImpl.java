package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ClientException;
import io.xpipe.beacon.exchange.SinkExchange;
import io.xpipe.core.store.ShellStore;

public class SinkExchangeImpl extends SinkExchange implements MessageExchangeImpl<SinkExchange.Request, SinkExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = getStoreEntryById(msg.getSource(), false);

        if (!(ds.getStore() instanceof ShellStore)) {
            throw new ClientException("Can't open file system for connection");
        }

        ShellStore store = ds.getStore().asNeeded();
        try (var fs = store.createFileSystem(); var inputStream = handler.receiveBody(); var output = fs.openOutput(msg.getPath())) {
            inputStream.transferTo(output);
        }

        return Response.builder().build();
    }
}
