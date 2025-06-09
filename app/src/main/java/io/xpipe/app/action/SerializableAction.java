package io.xpipe.app.action;

import io.xpipe.app.util.DataStoreFormatter;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.SuperBuilder;

import java.util.*;

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
            var value = property.getValue().asText();
            if (!value.isEmpty()) {
                map.put(name, value);
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
            }
        }
        return map;
    }
}
