package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.CategoryRemoveExchange;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.UUID;

public class CategoryRemoveExchangeImpl extends CategoryRemoveExchange {

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
}
