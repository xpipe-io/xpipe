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

import java.util.UUID;

public class CategoryAddExchange extends BeaconInterface<CategoryAddExchange.Request> {

    @Override
    public String getPath() {
        return "/category/add";
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws Throwable {
        if (DataStorage.get().getStoreCategoryIfPresent(msg.getParent()).isEmpty()) {
            throw new BeaconClientException("Parent category with id " + msg.getParent() + " does not exist");
        }

        var found = DataStorage.get().getStoreCategories().stream()
                .filter(dataStoreCategory -> msg.getParent().equals(dataStoreCategory.getParentCategory())
                        && msg.getName().equals(dataStoreCategory.getName()))
                .findAny();
        if (found.isPresent()) {
            return Response.builder().category(found.get().getUuid()).build();
        }

        var cat = DataStoreCategory.createNew(msg.getParent(), msg.getName());
        DataStorage.get().addStoreCategory(cat);
        return Response.builder().category(cat.getUuid()).build();
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
        String name;

        @NonNull
        UUID parent;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        UUID category;
    }
}
