package io.xpipe.app.exchange.cli;

import io.xpipe.app.exchange.MessageExchangeImpl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconHandler;
import io.xpipe.beacon.ServerException;
import io.xpipe.beacon.exchange.cli.ReadDrainExchange;
import io.xpipe.core.impl.SinkDrainStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.extension.util.ThreadHelper;

import java.io.InputStream;
import java.util.Optional;

public class ReadDrainExchangeImpl extends ReadDrainExchange
        implements MessageExchangeImpl<ReadDrainExchange.Request, ReadDrainExchange.Response> {

    @Override
    public Response handleRequest(BeaconHandler handler, Request msg) throws Exception {
        var ds = DataStorage.get().getStoreIfPresent(msg.getName());
        if (ds.isEmpty()) {
            ds = Optional.of(DataStorage.get().addStore(msg.getName(), msg.getStore()));
        }

        if (!(ds.get().getStore() instanceof SinkDrainStore)) {
            throw new ServerException("Data store is not a drain");
        }

        DataStoreEntry finalDs = ds.get();
        handler.postResponse(() -> {
            ThreadHelper.runFailableAsync(() -> {
                StreamDataStore store = finalDs.getStore().asNeeded();
                try (var output = handler.sendBody();
                        InputStream inputStream = store.openInput()) {
                    inputStream.transferTo(output);
                }
            });
        });
        return ReadDrainExchange.Response.builder().build();
    }
}
