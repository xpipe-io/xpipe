package io.xpipe.app.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class DataStorageParser {

    public static DataSource<?> sourceFromNode(JsonNode node) {
        node = replaceReferenceIds(node);
        var mapper = JacksonMapper.newMapper();
        try {
            return mapper.treeToValue(node, DataSource.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static DataStore storeFromNode(JsonNode node) {
        node = replaceReferenceIds(node);
        var mapper = JacksonMapper.newMapper();
        try {
            return mapper.treeToValue(node, DataStore.class);
        } catch (JsonProcessingException e) {
            ErrorEvent.fromThrowable(e).handle();
            return null;
        }
    }

    private static JsonNode replaceReferenceIds(JsonNode node) {
        return replaceReferenceIds(node, new HashSet<>());
    }

    private static JsonNode replaceReferenceIds(JsonNode node, Set<UUID> seenIds) {
        var mapper = JacksonMapper.newMapper();

        node = replaceReferenceIdsForType(node, "storeId", id -> {
            if (seenIds.contains(id)) {
                TrackEvent.withWarn("storage", "Encountered cycle").tag("id", id);
                return Optional.empty();
            }

            var entry = DataStorage.get().getStoreEntryByUuid(id);
            if (entry.isEmpty()) {
                TrackEvent.withWarn("storage", "Encountered unknown store").tag("id", id);
                return Optional.empty();
            }

            var storeNode = entry.get().getResolvedNode();
            if (storeNode == null) {
                TrackEvent.withWarn("storage", "Encountered disabled store").tag("id", id);
                return Optional.empty();
            }

            var newSeenIds = new HashSet<>(seenIds);
            newSeenIds.add(id);
            return Optional.of(replaceReferenceIds(storeNode, newSeenIds));
        });

        node = replaceReferenceIdsForType(node, "sourceId", id -> {
            var foundEntry = DataStorage.get().getSourceEntryByUuid(id);
            if (foundEntry.isPresent()) {
                var sourceNode = mapper.valueToTree(foundEntry.get().getSource());
                // return Optional.of(resolvesReferenceIds(sourceNode));
            }
            return Optional.empty();
        });

        return node;
    }

    private static JsonNode replaceReferenceIdsForType(
            JsonNode node, String replacementKeyName, Function<UUID, Optional<JsonNode>> function) {
        var value = getReferenceIdFromNode(node, replacementKeyName).orElse(null);
        if (value != null) {
            var found = function.apply(value);
            return found.orElseGet(NullNode::getInstance);
        }

        if (!node.isObject()) {
            return node;
        }

        var replacement = JsonNodeFactory.instance.objectNode();
        var iterator = node.fields();
        while (iterator.hasNext()) {
            var stringJsonNodeEntry = iterator.next();
            var resolved = replaceReferenceIdsForType(stringJsonNodeEntry.getValue(), replacementKeyName, function);
            replacement.set(stringJsonNodeEntry.getKey(), resolved);
        }
        return replacement;
    }

    private static Optional<UUID> getReferenceIdFromNode(JsonNode node, String key) {
        if (node.isObject()) {
            var found = node.get(key);
            if (found != null && found.isTextual()) {
                var id = UUID.fromString(found.textValue());
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
