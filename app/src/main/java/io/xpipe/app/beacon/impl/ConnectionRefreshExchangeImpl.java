package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionRefreshExchange;

public class ConnectionRefreshExchangeImpl extends ConnectionRefreshExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection: " + msg.getConnection()));
        if (e.getStore() instanceof FixedHierarchyStore) {
            DataStorage.get().refreshChildren(e, true);
        } else {
            e.validateOrThrow();
        }
        return Response.builder().build();
    }
}
