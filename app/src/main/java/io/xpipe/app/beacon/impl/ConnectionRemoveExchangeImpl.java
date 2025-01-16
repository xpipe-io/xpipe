package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionRemoveExchange;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.UUID;

public class ConnectionRemoveExchangeImpl extends ConnectionRemoveExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var entries = new ArrayList<DataStoreEntry>();
        for (UUID uuid : msg.getConnections()) {
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(uuid)
                    .orElseThrow(() -> new BeaconClientException("Unknown connection: " + uuid));
            entries.add(e);
        }
        DataStorage.get().deleteWithChildren(entries.toArray(DataStoreEntry[]::new));
        return Response.builder().build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
