package io.xpipe.app.beacon.api;

import com.sun.net.httpserver.HttpExchange;
import io.xpipe.app.beacon.BeaconClientException;
import io.xpipe.app.beacon.BeaconInterface;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.StorePath;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategoryInfoExchange extends BeaconInterface<CategoryInfoExchange.Request> {

    @Override
    public String getPath() {
        return "/category/info";
    }

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

    @Jacksonized
    @Builder
    @Value
    public static class Request {
        @NonNull
        List<UUID> categories;
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
        UUID category;

        UUID parentCategory;

        @NonNull
        StorePath name;

        @NonNull
        Instant lastUsed;

        @NonNull
        Instant lastModified;

        @NonNull
        JsonNode config;
    }
}
