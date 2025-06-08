package io.xpipe.app.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.hub.action.BatchStoreAction;
import io.xpipe.app.hub.action.MultiStoreAction;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;

import java.util.ArrayList;

public class ActionJacksonMapper {

    @SuppressWarnings("unchecked")
    public static <T extends AbstractAction> T parse(JsonNode tree) throws JsonProcessingException {
        if (!tree.isObject()) {
            return null;
        }

        var id = tree.get("id");
        if (id == null || !id.isTextual()) {
            return null;
        }

        var provider = ActionProvider.ALL.stream().filter(actionProvider -> id.textValue().equals(actionProvider.getId())).findFirst();
        if (provider.isEmpty()) {
            return null;
        }

        var clazz = provider.get().getActionClass();
        if (clazz.isEmpty()) {
            return null;
        }

        var object = (ObjectNode) tree;
        var ref = tree.get("ref");

        if (ref != null && !ref.isArray() && StoreAction.class.isAssignableFrom(clazz.get())) {
            var action = JacksonMapper.getDefault().treeToValue(tree, clazz.get());
            return (T) action;
        }

        var makeBatch = ref != null && ref.isArray() && !MultiStoreAction.class.isAssignableFrom(clazz.get());
        if (makeBatch) {
            if (ref.size() == 0) {
                return null;
            }

            var batchActions = new ArrayList<StoreAction<DataStore>>();
            object.remove("ref");
            for (JsonNode batchRef : ref) {
                object.set("ref", batchRef);
                var action = JacksonMapper.getDefault().treeToValue(object, clazz.get());
                batchActions.add((StoreAction<DataStore>) action);
            }
            return (T) BatchStoreAction.builder().actions(batchActions).build();
        }

        var makeMulti = ref != null && ref.isArray() && MultiStoreAction.class.isAssignableFrom(clazz.get());
        if (makeMulti) {
            object.remove("ref");
            object.set("refs", ref);
            var action = JacksonMapper.getDefault().treeToValue(object, clazz.get());
            return (T) action;
        }

        return null;
    }

    public static ObjectNode write(AbstractAction value) {
        if (value instanceof BatchStoreAction<?> b) {
            var arrayNode = JsonNodeFactory.instance.arrayNode();
            b.getActions().stream().map(a -> {
                var tree = (ObjectNode) JacksonMapper.getDefault().valueToTree(a);
                return tree.get("ref");
            }).forEach(n -> arrayNode.add(n));
            var tree = (ObjectNode) JacksonMapper.getDefault().valueToTree(b.getActions().getFirst());
            tree.set("ref", arrayNode);
            tree.put("id", b.getActions().getFirst().getId());
            return tree;
        }

        var tree = (ObjectNode) JacksonMapper.getDefault().valueToTree(value);

        if (value instanceof MultiStoreAction<?> m) {
            var refs = tree.get("refs");
            tree.remove("refs");
            tree.set("ref", refs);
            tree.put("id", m.getId());
            return tree;
        }

        tree.put("id", value.getId());
        return tree;
    }
}
