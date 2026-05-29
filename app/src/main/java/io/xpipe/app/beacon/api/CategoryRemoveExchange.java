package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoryRemoveExchange extends BeaconInterface<CategoryRemoveExchange.Request> {

    @Override
    public String getPath() {
        return "/category/remove";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var toRemove = new ArrayList<DataStoreCategory>();
        for (UUID uuid : msg.getCategories()) {
            var cat = DataStorage.get()
                    .getStoreCategoryIfPresent(uuid)
                    .orElseThrow(() -> new BeaconClientException("Unknown category: " + uuid));

            if (!DataStorage.get().canDeleteStoreCategory(cat)) {
                throw new BeaconClientException("Cannot delete category: " + cat.getName());
            }

            toRemove.add(cat);
        }

        for (DataStoreCategory cat : toRemove) {
            DataStorage.get().deleteStoreCategory(cat, msg.isRemoveChildrenCategories(), msg.isRemoveContents());
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
        List<UUID> categories;

        boolean removeChildrenCategories;

        boolean removeContents;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {}
}
