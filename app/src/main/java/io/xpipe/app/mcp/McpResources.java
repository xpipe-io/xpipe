package io.xpipe.app.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.beacon.BeaconClientException;
import io.xpipe.beacon.api.CategoryInfoExchange;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.StorePath;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.ClassUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class McpResources {

    @Jacksonized
    @Builder
    @Value
    public static class ConnectionResource {
        @NonNull
        StorePath name;

        @NonNull
        StorePath category;

        @NonNull
        String type;

        @NonNull
        Object connectionData;

        @NonNull
        Object usageCategory;

        @NonNull
        Instant lastUsed;

        @NonNull
        Instant lastModified;

        @NonNull
        Object internalState;

        @NonNull
        Map<String, Object> internalCache;
    }


    @Jacksonized
    @Builder
    @Value
    public static class CategoryResource {
        @NonNull
        StorePath name;

        @NonNull
        Instant lastUsed;

        @NonNull
        Instant lastModified;

        @NonNull
        JsonNode config;
    }

    public static McpServerFeatures.SyncResourceSpecification connections() throws IOException {
        McpSchema.Annotations annotations = new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), 1.0);
        var resource = McpSchema.Resource.builder()
                .uri("xpipe://connections")
                .name("xpipe connections")
                .description("Available connections in xpipe")
                .mimeType("application/json")
                .annotations(annotations)
                .build();
        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) -> {
            var list = new ArrayList<McpSchema.ResourceContents>();
            for (var e : DataStorage.get().getStoreEntries()) {
                if (!e.getValidity().isUsable()) {
                    continue;
                }

                var names = DataStorage.get().getStorePath(DataStorage.get().getStoreCategoryIfPresent(e.getCategoryUuid()).orElseThrow()).getNames();
                var cat = new StorePath(names.subList(1, names.size()));
                var cache = e.getStoreCache().entrySet().stream().filter(stringObjectEntry -> {
                    return stringObjectEntry.getValue() != null && (ClassUtils.isPrimitiveOrWrapper(stringObjectEntry.getValue().getClass()) ||
                            stringObjectEntry.getValue() instanceof String);
                }).collect(Collectors.toMap(stringObjectEntry -> stringObjectEntry.getKey(), stringObjectEntry -> stringObjectEntry.getValue()));

                var resourceData = ConnectionResource.builder().lastModified(e.getLastModified()).lastUsed(e.getLastUsed())
                        .category(cat).name(DataStorage.get().getStorePath(e)).connectionData(e.getStore()).usageCategory(
                        e.getProvider().getUsageCategory()).type(e.getProvider().getId()).internalState(
                        e.getStorePersistentState() != null ? e.getStorePersistentState() : new Object()).internalCache(cache).build();

                McpSchema.TextResourceContents c;
                try {
                    c = new McpSchema.TextResourceContents("xpipe://connections/" + e.getUuid(), "application/json",
                            JacksonMapper.getDefault().writeValueAsString(resourceData));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                list.add(c);
            }

            return new McpSchema.ReadResourceResult(list);
        });
    }


    public static McpServerFeatures.SyncResourceSpecification categories() throws IOException {
        McpSchema.Annotations annotations = new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), 0.3);
        var resource = McpSchema.Resource.builder()
                .uri("xpipe://categories")
                .name("xpipe categories")
                .description("Available categories in xpipe")
                .mimeType("application/json")
                .annotations(annotations)
                .build();
        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) -> {
            var list = new ArrayList<McpSchema.ResourceContents>();
            for (var cat : DataStorage.get().getStoreCategories()) {
                var name = DataStorage.get().getStorePath(cat);
                var jsonData = CategoryResource.builder()
                        .lastModified(cat.getLastModified())
                        .lastUsed(cat.getLastUsed())
                        .name(name)
                        .config(JacksonMapper.getDefault().valueToTree(cat.getConfig()))
                        .build();

                McpSchema.TextResourceContents c;
                try {
                    c = new McpSchema.TextResourceContents("xpipe://categories/" + cat.getUuid(), "application/json",
                            JacksonMapper.getDefault().writeValueAsString(jsonData));
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                list.add(c);
            }

            return new McpSchema.ReadResourceResult(list);
        });
    }
}
