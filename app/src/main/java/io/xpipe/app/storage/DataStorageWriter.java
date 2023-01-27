package io.xpipe.app.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class DataStorageWriter {

    public static JsonNode storeToNode(DataStore store) {
        var mapper = JacksonMapper.newMapper();
        var tree = mapper.valueToTree(store);
        return replaceReferencesWithIds(store, tree);
    }

    public static JsonNode sourceToNode(DataSource<?> source) {
        var mapper = JacksonMapper.newMapper();
        var tree = mapper.valueToTree(source);
        return replaceReferencesWithIds(source, tree);
    }

    private static JsonNode replaceReferencesWithIds(Object root, JsonNode node) {
        var mapper = JacksonMapper.newMapper();

        node = replaceReferencesWithIds(
                node,
                possibleReference -> {
                    if (!possibleReference.isObject()) {
                        return Optional.empty();
                    }

                    try {
                        var store = mapper.treeToValue(possibleReference, DataStore.class);
                        if (root == null || !root.equals(store)) {
                            var found = DataStorage.get().getEntryByStore(store);
                            return found.map(dataSourceEntry -> dataSourceEntry.getUuid());
                        }
                    } catch (JsonProcessingException e) {
                    }
                    return Optional.empty();
                },
                "storeId");

        node = replaceReferencesWithIds(
                node,
                possibleReference -> {
                    try {
                        var source = mapper.treeToValue(possibleReference, DataSource.class);
                        if (root == null || !root.equals(source)) {
                            var found = DataStorage.get().getEntryBySource(source);
                            return found.map(dataSourceEntry -> dataSourceEntry.getUuid());
                        }
                    } catch (JsonProcessingException e) {
                    }
                    return Optional.empty();
                },
                "sourceId");

        return node;
    }

    private static JsonNode replaceReferencesWithIds(
            JsonNode node, Function<JsonNode, Optional<UUID>> function, String key) {
        if (!node.isObject()) {
            return node;
        }

        var value = function.apply(node).orElse(null);
        if (value != null) {
            var idReplacement = JsonNodeFactory.instance.objectNode().set(key, new TextNode(value.toString()));
            return idReplacement;
        }

        var replacement = JsonNodeFactory.instance.objectNode();
        node.fields().forEachRemaining(stringJsonNodeEntry -> {
            var resolved = replaceReferencesWithIds(null, stringJsonNodeEntry.getValue());
            replacement.set(stringJsonNodeEntry.getKey(), resolved);
        });
        return replacement;
    }
}
