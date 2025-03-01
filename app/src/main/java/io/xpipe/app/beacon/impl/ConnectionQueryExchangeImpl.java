package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.beacon.api.ConnectionQueryExchange;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConnectionQueryExchangeImpl extends ConnectionQueryExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var found = DataStorageQuery.query(msg.getCategoryFilter(), msg.getConnectionFilter(), msg.getTypeFilter());
        return Response.builder()
                .found(found.stream().map(entry -> entry.getUuid()).toList())
                .build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
