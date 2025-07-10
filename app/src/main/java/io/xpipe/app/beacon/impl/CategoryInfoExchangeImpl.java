package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.CategoryInfoExchange;
import io.xpipe.core.JacksonMapper;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.UUID;

public class CategoryInfoExchangeImpl extends CategoryInfoExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var list = new ArrayList<InfoResponse>();
        for (UUID uuid : msg.getCategories()) {
            var cat = DataStorage.get()
                    .getStoreCategoryIfPresent(uuid)
                    .orElseThrow(() -> new BeaconClientException("Unknown category: " + uuid));

            var name = DataStorage.get().getStorePath(cat);

            var apply = InfoResponse.builder()
                    .lastModified(cat.getLastModified())
                    .lastUsed(cat.getLastUsed())
                    .category(cat.getUuid())
                    .parentCategory(cat.getParentCategory())
                    .name(name)
                    .config(JacksonMapper.getDefault().valueToTree(cat.getConfig()))
                    .build();
            list.add(apply);
        }
        return Response.builder().infos(list).build();
    }

    @Override
    public Object getSynchronizationObject() {
        return DataStorage.get();
    }
}
