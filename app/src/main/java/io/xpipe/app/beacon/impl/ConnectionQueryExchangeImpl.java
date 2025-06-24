package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.beacon.api.ConnectionQueryExchange;

import com.sun.net.httpserver.HttpExchange;

public class ConnectionQueryExchangeImpl extends ConnectionQueryExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var found =
                DataStorageQuery.queryEntry(msg.getCategoryFilter(), msg.getConnectionFilter(), msg.getTypeFilter());
        return Response.builder()
                .found(found.stream().map(entry -> entry.getUuid()).toList())
                .build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
