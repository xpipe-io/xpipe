package io.xpipe.app.action;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.core.UuidHelper;
import lombok.experimental.SuperBuilder;

import java.util.*;
import java.util.stream.Collectors;

@SuperBuilder
public abstract class SerializableAction extends AbstractAction {

    public String toString() {
        return toNode().toPrettyString();
    }

    public ObjectNode toNode() {
        var json = ActionJacksonMapper.write(this);
        return json;
    }

    public ObjectNode toConfigNode() {
        var json = toNode();
        json.remove("ref");
        json.remove("refs");
        return json;
    }

    public Optional<? extends SerializableAction> withConfigString(String configString) {
        try {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(configString);
            tree.put("id", getId());
            SerializableAction action = ActionJacksonMapper.parse(tree);
            return Optional.ofNullable(action);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> toDisplayMap() {
        var node = toConfigNode();

        var map = new LinkedHashMap<String, String>();
        map.put("Action", getDisplayName());
        for (Map.Entry<String, JsonNode> property : node.properties()) {
            if (property.getKey().equals("id")) {
                continue;
            }

            var name = DataStoreFormatter.camelCaseToName(property.getKey());
            name = Arrays.stream(name.split(" ")).filter(s -> !s.equals("Store")).collect(Collectors.joining(" "));

            if (property.getValue().isTextual()) {
                var value = property.getValue().textValue();
                var uuid = UuidHelper.parse(value);
                if (uuid.isPresent()) {
                    var refName = DataStorage.get().getStoreEntryIfPresent(uuid.get()).map(e -> e.getName()).or(() -> {
                        return DataStorage.get().getStoreCategoryIfPresent(uuid.get()).map(c -> c.getName());
                    });
                    map.put(name, refName.orElse(value));
                } else {
                    map.put(name, value);
                }
            } else if (property.getValue().isArray()) {
                var list = new ArrayList<String>();
                for (JsonNode jsonNode : property.getValue()) {
                    var s = jsonNode.asText();
                    if (!s.isEmpty()) {
                        list.add(s);
                    }
                }

                if (!list.isEmpty()) {
                    map.put(name, String.join("\n", list));
                }
            } else if (property.getValue().isBoolean()) {
                map.put(name, property.getValue().booleanValue() ? "Yes" : "No");
            } else {
                var value = property.getValue().asText();
                map.put(name, value);
            }
        }
        return map;
    }
}
