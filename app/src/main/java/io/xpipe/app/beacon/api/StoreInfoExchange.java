package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.StorePath;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.ClassUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class StoreInfoExchange extends BeaconInterface<StoreInfoExchange.Request> {

    @Override
    public String getPath() {
        return "/store/info";
    }

    @Override
    public List<String> getPathAliases() {
        return List.of("/connection/info");
    }

    @Override
    public Object handle(HttpExchange exchange, Request msg) throws BeaconClientException {
        var list = new ArrayList<InfoResponse>();
        for (UUID uuid : msg.getStores()) {
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(uuid)
                    .orElseThrow(() -> new BeaconClientException("Unknown  store: " + uuid));

            var names = DataStorage.get()
                    .getStorePath(DataStorage.get()
                            .getStoreCategoryIfPresent(e.getCategoryUuid())
                            .orElseThrow())
                    .getNames();
            var cat = new StorePath(names.subList(1, names.size()));
            var cache = e.getStoreCache().entrySet().stream()
                    .filter(kv -> {
                        return kv.getValue() != null
                                && (ClassUtils.isPrimitiveOrWrapper(
                                                kv.getValue().getClass())
                                        || kv.getValue() instanceof String);
                    })
                    .collect(Collectors.toMap(
                            stringObjectEntry -> stringObjectEntry.getKey(),
                            stringObjectEntry -> stringObjectEntry.getValue()));

            var apply = InfoResponse.builder()
                    .lastModified(e.getLastModified())
                    .lastUsed(e.getLastUsed())
                    .store(e.getUuid())
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

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        List<UUID> stores;
    }

    @Jacksonized
    @Builder
    @Value
    public static class Response {
        @NonNull
        List<@NonNull InfoResponse> infos;
    }

    @Jacksonized
    @Builder
    @Value
    public static class InfoResponse {
        @NonNull
        UUID store;

        @NonNull
        StorePath category;

        @NonNull
        StorePath name;

        @NonNull
        String type;

        @NonNull
        Object rawData;

        @NonNull
        Object usageCategory;

        @NonNull
        Instant lastUsed;

        @NonNull
        Instant lastModified;

        @NonNull
        Object state;

        @NonNull
        Map<String, Object> cache;
    }
}
