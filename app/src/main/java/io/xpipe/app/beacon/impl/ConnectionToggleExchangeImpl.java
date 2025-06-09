package io.xpipe.app.beacon.impl;

import io.xpipe.app.ext.SingletonSessionStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionToggleExchange;

import com.sun.net.httpserver.HttpExchange;

public class ConnectionToggleExchangeImpl extends ConnectionToggleExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Exception {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection: " + msg.getConnection()));
        if (!(e.getStore() instanceof SingletonSessionStore<?> singletonSessionStore)) {
            throw new BeaconClientException("Not a toggleable connection");
        }
        if (msg.getState()) {
            singletonSessionStore.startSessionIfNeeded();
        } else {
            singletonSessionStore.stopSessionIfNeeded();
        }
        return Response.builder().build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
