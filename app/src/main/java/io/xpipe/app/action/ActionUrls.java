package io.xpipe.app.action;

import io.xpipe.app.util.Base64Helper;
import io.xpipe.app.util.JacksonMapper;

import lombok.SneakyThrows;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

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
        if (node.isString()) {
            return List.of(encodeValue(node.asString()));
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

        var enc = Base64Helper.toBase64Url(node.toPrettyString());
        return List.of("~" + enc);
    }

    @SneakyThrows
    public static String toUrl(AbstractAction action) {
        if (!(action instanceof SerializableAction sa)) {
            return null;
        }

        var json = sa.toNode();
        var parsed =
                JacksonMapper.getDefault().treeToValue(json, new TypeReference<LinkedHashMap<String, JsonNode>>() {});

        Map<String, List<String>> requestParams = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> e : parsed.entrySet()) {
            var value = nodeToString(e.getValue());
            requestParams.put(e.getKey(), value);
        }

        String encodedURL = requestParams.keySet().stream()
                .map(key -> {
                    var vals = requestParams.get(key);
                    return vals.stream().map(s -> key + "=" + s).collect(Collectors.joining("&"));
                })
                .collect(Collectors.joining("&", "xpipe://action?", ""));
        return encodedURL;
    }

    public static Optional<AbstractAction> parse(String queryString) {
        var query = splitQuery(queryString);

        var id = query.get("id");
        if (id == null || id.size() != 1) {
            return Optional.empty();
        }

        var provider = ActionProvider.ALL.stream()
                .filter(actionProvider -> id.getFirst().equals(actionProvider.getId()))
                .findFirst();
        if (provider.isEmpty()) {
            return Optional.empty();
        }

        var clazz = provider.get().getActionClass();
        if (clazz.isEmpty()) {
            return Optional.empty();
        }

        if (!SerializableAction.class.isAssignableFrom(clazz.get())) {
            return Optional.empty();
        }

        var stores = query.get("ref");
        if (stores == null || stores.isEmpty()) {
            return Optional.empty();
        }

        var fixedMap = new LinkedHashMap<String, Object>();
        for (var entry : query.entrySet()) {
            var list = new ArrayList<>();
            for (String s : entry.getValue()) {
                if (s.startsWith("~")) {
                    var json = Base64Helper.fromBase64UrlString(s.substring(1));
                    var node = JacksonMapper.getDefault().readTree(json);
                    list.add(node);
                } else {
                    list.add(s);
                }
            }

            var unwrapped = list.size() == 1 ? list.getFirst() : list;
            fixedMap.put(entry.getKey(), unwrapped);
        }

        var json = (ObjectNode) JacksonMapper.getDefault().valueToTree(fixedMap);
        var instance = ActionJacksonMapper.parse(json);
        return Optional.ofNullable(instance);
    }

    private static Map<String, List<String>> splitQuery(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }

        return Arrays.stream(query.split("&"))
                .map(ActionUrls::splitQueryParameter)
                .collect(Collectors.groupingBy(
                        AbstractMap.SimpleImmutableEntry::getKey,
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    private static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
        return new AbstractMap.SimpleImmutableEntry<>(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null);
    }
}
