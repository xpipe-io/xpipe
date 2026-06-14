package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageQuery;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

public class StoreQueryExchange extends BeaconInterface<StoreQueryExchange.Request> {

    @Override
    public String getPath() {
        return "/store/query";
    }

    @Override
    public List<String> getPathAliases() {
        return List.of("/connection/query");
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) {
        var found =
                DataStorageQuery.queryEntry(msg.getCategoryFilter(), msg.getStoreFilter(), msg.getTypeFilter());
        return Response.builder()
                .found(found.stream().map(entry -> entry.getUuid()).toList())
                .build();
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
        String categoryFilter;

        @NonNull
        String storeFilter;

        @NonNull
        String typeFilter;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        List<@NonNull UUID> found;
    }
}
