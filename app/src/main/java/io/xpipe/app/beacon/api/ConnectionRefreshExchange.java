package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.ext.FixedHierarchyStore;
import io.xpipe.app.storage.DataStorage;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

public class ConnectionRefreshExchange extends BeaconInterface<ConnectionRefreshExchange.Request> {

    @Override
    public String getPath() {
        return "/connection/refresh";
    }

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

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        UUID connection;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
