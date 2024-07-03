package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionAddExchange;

import java.util.UUID;

public class ConnectionAddExchangeImpl extends ConnectionAddExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var cat = msg.getCategory() != null ? msg.getCategory() : DataStorage.DEFAULT_CATEGORY_UUID;
        var entry = DataStorage.get().addStoreEntryIfNotPresent(DataStoreEntry.createNew(UUID.randomUUID(), cat, msg.getName(), msg.getData()));
        return Response.builder().connection(entry.getUuid()).build();
    }
}
