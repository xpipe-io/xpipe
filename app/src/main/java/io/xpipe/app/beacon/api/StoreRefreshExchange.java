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

import java.util.List;
import java.util.UUID;

public class StoreRefreshExchange extends BeaconInterface<StoreRefreshExchange.Request> {

    @Override
    public String getPath() {
        return "/store/refresh";
    }

    @Override
    public List<String> getPathAliases() {
        return List.of("/connection/refresh");
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getStore())
                .orElseThrow(() -> new BeaconClientException("Unknown  store: " + msg.getStore()));
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
        UUID store;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
