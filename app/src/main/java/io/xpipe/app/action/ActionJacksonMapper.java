package io.xpipe.app.action;

import io.xpipe.app.hub.action.*;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.UuidHelper;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

public class ActionJacksonMapper {

    @SuppressWarnings("unchecked")
    public static <T extends AbstractAction> T parse(JsonNode tree) {
        if (!tree.isObject()) {
            return null;
        }

        var id = tree.get("id");
        if (id == null || !id.isString()) {
            return null;
        }

        var provider = ActionProvider.ALL.stream()
                .filter(actionProvider -> id.stringValue().equals(actionProvider.getId()))
                .findFirst();
        if (provider.isEmpty()) {
            return null;
        }

        var clazz = provider.get().getActionClass();
        if (clazz.isEmpty()) {
            return null;
        }

        var object = (ObjectNode) tree;
        var ref = tree.get("ref");

        var mapper = JacksonMapper.getDefault()
                .rebuild()
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();

        if (ref != null && !ref.isArray() && StoreAction.class.isAssignableFrom(clazz.get())) {
            validateRef(provider.get(), ref.asString());
            var action = mapper.treeToValue(tree, clazz.get());
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
                validateRef(provider.get(), batchRef.asString());
                object.set("ref", batchRef);
                var action = mapper.treeToValue(object, clazz.get());
                batchActions.add((StoreAction<DataStore>) action);
            }
            return (T) BatchStoreAction.builder().actions(batchActions).build();
        }

        var makeMulti = ref != null && ref.isArray() && MultiStoreAction.class.isAssignableFrom(clazz.get());
        if (makeMulti) {
            validateRef(provider.get(), ref.asString());
            object.remove("ref");
            object.set("refs", ref);
            var action = mapper.treeToValue(object, clazz.get());
            return (T) action;
        }

        return null;
    }

    private static void validateRef(ActionProvider provider, String ref) {
        var uuid = UuidHelper.parse(ref);
        if (uuid.isEmpty()) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("Invalid store id: " + ref));
        }

        var entry = DataStorage.get().getStoreEntryIfPresent(uuid.get());
        if (entry.isEmpty()) {
            throw ErrorEventFactory.expected(new IllegalArgumentException("Store not found for id: " + ref));
        }

        if (!entry.get().getValidity().isUsable()) {
            throw ErrorEventFactory.expected(new IllegalArgumentException(
                    "Store " + DataStorage.get().getStorePath(entry.get()) + " is incomplete"));
        }

        if (provider instanceof HubLeafProvider<?> l
                && (!l.getApplicableClass()
                                .isAssignableFrom(entry.get().getStore().getClass())
                        || !l.isApplicable(entry.get().ref()))) {
            throw ErrorEventFactory.expected(new IllegalArgumentException(
                    "Store " + DataStorage.get().getStorePath(entry.get()) + " is not applicable for action type"));
        }

        if (provider instanceof BatchHubProvider<?> h
                && (!h.getApplicableClass()
                                .isAssignableFrom(entry.get().getStore().getClass())
                        || !h.isActive(entry.get().ref())
                        || !h.isApplicable(entry.get().ref()))) {
            throw ErrorEventFactory.expected(new IllegalArgumentException(
                    "Store " + DataStorage.get().getStorePath(entry.get()) + " is not applicable for action type"));
        }
    }

    public static ObjectNode write(AbstractAction value) {
        if (value instanceof BatchStoreAction<?> b) {
            var arrayNode = JsonNodeFactory.instance.arrayNode();
            b.getActions().stream()
                    .map(a -> {
                        var tree = (ObjectNode) JacksonMapper.getDefault().valueToTree(a);
                        return tree.get("ref");
                    })
                    .forEach(n -> arrayNode.add(n));
            var tree = (ObjectNode)
                    JacksonMapper.getDefault().valueToTree(b.getActions().getFirst());
            tree.set("ref", arrayNode);
            tree.put("id", b.getActions().getFirst().getId());
            return tree;
        }

        var tree = (ObjectNode) JacksonMapper.getDefault().valueToTree(value);
        var treeCopy = JsonNodeFactory.instance.objectNode();
        treeCopy.put("id", value.getId());
        tree.properties().forEach(p -> {
            treeCopy.set(p.getKey(), p.getValue());
        });

        if (value instanceof MultiStoreAction<?> m) {
            var refs = treeCopy.get("refs");
            treeCopy.remove("refs");
            treeCopy.set("ref", refs);
            treeCopy.put("id", m.getId());
            return treeCopy;
        }

        return treeCopy;
    }
}
