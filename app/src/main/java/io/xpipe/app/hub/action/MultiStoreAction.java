package io.xpipe.app.hub.action;

import io.xpipe.app.action.ActionJacksonMapper;
import io.xpipe.app.action.SerializableAction;
import io.xpipe.app.action.StoreContextAction;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuperBuilder
public abstract class MultiStoreAction<T extends DataStore> extends SerializableAction implements StoreContextAction {

    @Getter
    protected final List<DataStoreEntryRef<T>> refs;

    @Override
    public String getShortcutName() {
        var names = refs.size() > 3
                ? refs.size() + " connections"
                : refs.stream()
                        .map(ref -> DataStorage.get().getStoreEntryDisplayName(ref.get()))
                        .collect(Collectors.joining(", "));
        return names + " (" + getDisplayName() + ")";
    }

    @Override
    protected void beforeExecute() {
        for (DataStoreEntryRef<T> ref : refs) {
            ref.get().incrementBusyCounter();
        }
    }

    @Override
    protected void afterExecute() {
        for (DataStoreEntryRef<T> ref : refs) {
            ref.get().decrementBusyCounter();
        }
    }

    @SneakyThrows
    public MultiStoreAction<T> withRefs(List<DataStoreEntryRef<T>> refs) {
        var node = toNode();
        node.set("ref", JacksonMapper.getDefault().valueToTree(refs));
        MultiStoreAction<T> action = ActionJacksonMapper.parse(node);
        return action;
    }

    public Optional<MultiStoreAction<T>> withConfigString(String configString) {
        try {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(configString);
            tree.set("ref", JacksonMapper.getDefault().valueToTree(refs));
            tree.put("id", getId());
            MultiStoreAction<T> action = ActionJacksonMapper.parse(tree);
            return Optional.ofNullable(action);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<DataStoreEntry> getStoreEntryContext() {
        return refs.stream().map(ref -> ref.get()).toList();
    }
}
