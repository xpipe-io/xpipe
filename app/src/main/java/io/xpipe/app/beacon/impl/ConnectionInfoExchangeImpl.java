package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionInfoExchange;
import io.xpipe.core.store.StorePath;

import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.UUID;

public class ConnectionInfoExchangeImpl extends ConnectionInfoExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var list = new ArrayList<InfoResponse>();
        for (UUID uuid : msg.getConnections()) {
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(uuid)
                    .orElseThrow(() -> new BeaconClientException("Unknown connection: " + uuid));

            var names = DataStorage.get()
                    .getStorePath(DataStorage.get()
                            .getStoreCategoryIfPresent(e.getCategoryUuid())
                            .orElseThrow())
                    .getNames();
            var cat = new StorePath(names.subList(1, names.size()));

            var apply = InfoResponse.builder()
                    .lastModified(e.getLastModified())
                    .lastUsed(e.getLastUsed())
                    .connection(e.getCategoryUuid())
                    .category(cat)
                    .name(DataStorage.get().getStorePath(e))
                    .rawData(e.getStore())
                    .usageCategory(e.getProvider().getUsageCategory())
                    .type(e.getProvider().getId())
                    .state(e.getStorePersistentState())
                    .build();
            list.add(apply);
        }
        return Response.builder().infos(list).build();
    }
}
