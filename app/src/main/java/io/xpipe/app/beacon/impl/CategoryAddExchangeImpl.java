package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.CategoryAddExchange;

import com.sun.net.httpserver.HttpExchange;

public class CategoryAddExchangeImpl extends CategoryAddExchange {

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
}
