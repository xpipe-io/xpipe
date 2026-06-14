package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StoreRemoveExchange extends BeaconInterface<StoreRemoveExchange.Request> {

    @Override
    public String getPath() {
        return "/store/remove";
    }

    @Override
    public List<String> getPathAliases() {
        return List.of("/connection/remove");
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var entries = new ArrayList<DataStoreEntry>();
        for (UUID uuid : msg.getStores()) {
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(uuid)
                    .orElseThrow(() -> new BeaconClientException("Unknown  store: " + uuid));
            entries.add(e);
        }
        DataStorage.get().deleteWithChildren(entries.toArray(DataStoreEntry[]::new));
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
        List<UUID> stores;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
