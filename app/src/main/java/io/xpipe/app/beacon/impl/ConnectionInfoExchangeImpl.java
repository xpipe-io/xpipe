package io.xpipe.app.beacon.impl;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionInfoExchange;

public class ConnectionInfoExchangeImpl extends ConnectionInfoExchange {

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var e = DataStorage.get()
                .getStoreEntryIfPresent(msg.getConnection())
                .orElseThrow(() -> new BeaconClientException("Unknown connection"));

        return Response.builder().lastModified(e.getLastModified()).lastUsed(e.getLastUsed()).connection(e.getCategoryUuid()).category(DataStorage.get()
                .getStorePath(DataStorage.get().getStoreCategoryIfPresent(e.getCategoryUuid()).orElseThrow())).name(DataStorage.get().getStorePath(e)).rawData(e.getStore()).usageCategory(e.getProvider().getUsageCategory()).type(e.getProvider().getId()).build();
    }
}
