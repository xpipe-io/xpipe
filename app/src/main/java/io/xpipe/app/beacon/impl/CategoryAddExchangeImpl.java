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

        if (DataStorage.get().getStoreCategories().stream()
                .anyMatch(dataStoreCategory -> msg.getParent().equals(dataStoreCategory.getParentCategory())
                        && msg.getName().equals(dataStoreCategory.getName()))) {
            throw new BeaconClientException(
                    "Category with name " + msg.getName() + " already exists in parent category");
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
