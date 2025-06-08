package io.xpipe.app.action;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vladsch.flexmark.ast.Link;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.UuidHelper;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ActionUrls {

    private static String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static List<String> nodeToString(JsonNode node) {
        if (node.isTextual()) {
            return List.of(encodeValue(node.asText()));
        }

        if (node.isArray()) {
            var list = new ArrayList<String>();
            for (JsonNode c : node) {
                var r = nodeToString(c);
                if (r.size() == 1) {
                    list.add(r.getFirst());
                }
            }
            return list;
        }

        var enc = InPlaceSecretValue.of(node.toPrettyString()).getEncryptedValue();
        return List.of(enc);
    }

    @SneakyThrows
    public static String toUrl(AbstractAction action) {
        if (!(action instanceof SerializableAction sa)) {
            return null;
        }

        var json = sa.toNode();
        var parsed = JacksonMapper.getDefault().treeToValue(json, new TypeReference<LinkedHashMap<String, JsonNode>>() {});

        Map<String, List<String>> requestParams = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : parsed.entrySet()) {
            var value = nodeToString(e.getValue());
            requestParams.put(e.getKey(), value);
        }

        String encodedURL = requestParams.keySet().stream().map(key -> {
            var vals = requestParams.get(key);
            return vals.stream().map(s -> key + "=" + s).collect(Collectors.joining("&"));
        }).collect(
                Collectors.joining("&", "xpipe://action?", ""));
        return encodedURL;
    }

    public static List<AbstractAction> parse(String queryString) throws Exception {
        var query = splitQuery(queryString);

        var id = query.get("id");
        if (id == null || id.isEmpty()) {
            return List.of();
        }

        var stores = query.get("store");
        if (stores == null || stores.isEmpty()) {
            return List.of();
        }

        List<DataStoreEntry> entries = new ArrayList<>();
        for (String store : stores) {
            var uuid = UuidHelper.parse(store);
            if (uuid.isEmpty()) {
                throw new IllegalArgumentException("Invalid store id: " + store);
            }

            var entry = DataStorage.get()
                    .getStoreEntryIfPresent(uuid.get());
            if (entry.isEmpty()) {
                throw new IllegalArgumentException("Store not found for id: " + store);
            }

            if (!entry.get().getValidity().isUsable()) {
                throw new IllegalArgumentException("Store " + DataStorage.get().getStorePath(entry.get()) + " is incomplete");
            }

            entries.add(entry.get());
        }

        query.remove("id");
        query.remove("store");
        var json = (ObjectNode) JacksonMapper.getDefault().valueToTree(query);

        TrackEvent.withDebug("Parsed action")
                .tag("id", id)
                .tag("entries", entries)
                .tag("data", json.toPrettyString())
                .handle();

        var list = new ArrayList<AbstractAction>();
        for (String storeId : stores) {
            json.put("ref", storeId);
            AbstractAction instance = JacksonMapper.getDefault().treeToValue(json, AbstractAction.class);
            list.add(instance);
        }
        return list;
    }

    private static Map<String, List<String>> splitQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }

        return Arrays.stream(query.split("&"))
                .map(ActionUrls::splitQueryParameter)
                .collect(Collectors.groupingBy(AbstractMap.SimpleImmutableEntry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null
        );
    }
}
