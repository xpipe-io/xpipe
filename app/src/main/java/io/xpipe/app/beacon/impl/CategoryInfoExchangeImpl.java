package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.CategoryInfoExchange;
import io.xpipe.beacon.api.ConnectionInfoExchange;
import io.xpipe.core.store.StorePath;
import io.xpipe.core.util.JacksonMapper;
import org.apache.commons.lang3.ClassUtils;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

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
