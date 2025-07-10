package io.xpipe.app.beacon.impl;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.ConnectionInfoExchange;
import io.xpipe.core.StorePath;

import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.lang3.ClassUtils;

import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

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
            var cache = e.getStoreCache().entrySet().stream()
                    .filter(stringObjectEntry -> {
                        return stringObjectEntry.getValue() != null
                                && (ClassUtils.isPrimitiveOrWrapper(
                                                stringObjectEntry.getValue().getClass())
                                        || stringObjectEntry.getValue() instanceof String);
                    })
                    .collect(Collectors.toMap(
                            stringObjectEntry -> stringObjectEntry.getKey(),
                            stringObjectEntry -> stringObjectEntry.getValue()));

            var apply = InfoResponse.builder()
                    .lastModified(e.getLastModified())
                    .lastUsed(e.getLastUsed())
                    .connection(e.getUuid())
                    .category(cat)
                    .name(DataStorage.get().getStorePath(e))
                    .rawData(e.getStore())
                    .usageCategory(e.getProvider().getUsageCategory())
                    .type(e.getProvider().getId())
                    .state(e.getStorePersistentState() != null ? e.getStorePersistentState() : new Object())
                    .cache(cache)
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
