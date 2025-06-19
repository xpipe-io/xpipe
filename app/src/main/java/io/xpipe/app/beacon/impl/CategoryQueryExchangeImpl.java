package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import io.xpipe.beacon.api.CategoryQueryExchange;

public class CategoryQueryExchangeImpl extends CategoryQueryExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var found = DataStorageQuery.queryCategory(msg.getFilter());
        return Response.builder()
                .found(found.stream().map(entry -> entry.getUuid()).toList())
                .build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
