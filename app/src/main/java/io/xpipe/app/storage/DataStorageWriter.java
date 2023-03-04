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
        return replaceReferencesWithIds(tree, true);
    }

    public static JsonNode sourceToNode(DataSource<?> source) {
        var mapper = JacksonMapper.newMapper();
        var tree = mapper.valueToTree(source);
        return replaceReferencesWithIds(tree, true);
    }

    private static JsonNode replaceReferencesWithIds(JsonNode node, boolean isRoot) {
        var mapper = JacksonMapper.newMapper();

        node = replaceReferencesWithIds(
                node,
                possibleReference -> {
                    if (!possibleReference.isObject()) {
                        return Optional.empty();
                    }

                    try {
                        var store = mapper.treeToValue(possibleReference, DataStore.class);
                        if (store == null) {
                            return Optional.empty();
                        }

                        if (!isRoot) {
                            var found = DataStorage.get().getStoreEntryIfPresent(store);
                            return found.map(dataSourceEntry -> dataSourceEntry.getUuid());
                        }
                    } catch (JsonProcessingException e) {
                    }
                    return Optional.empty();
                },
                "storeId",
                isRoot);

        node = replaceReferencesWithIds(
                node,
                possibleReference -> {
                    try {
                        var source = mapper.treeToValue(possibleReference, DataSource.class);
                        if (!isRoot) {
                            var found = DataStorage.get().getSourceEntry(source);
                            return found.map(dataSourceEntry -> dataSourceEntry.getUuid());
                        }
                    } catch (JsonProcessingException e) {
                    }
                    return Optional.empty();
                },
                "sourceId",
                isRoot);

        return node;
    }

    private static JsonNode replaceReferencesWithIds(
            JsonNode node, Function<JsonNode, Optional<UUID>> function, String key, boolean isRoot) {
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
            var resolved = replaceReferencesWithIds(stringJsonNodeEntry.getValue(), false);
            replacement.set(stringJsonNodeEntry.getKey(), resolved);
        });
        return replacement;
    }
}
