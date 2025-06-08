package io.xpipe.app.hub.action;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.action.ActionJacksonMapper;
import io.xpipe.app.action.SerializableAction;
import io.xpipe.app.action.StoreContextAction;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;

@SuperBuilder
public abstract class StoreAction<T extends DataStore> extends SerializableAction implements StoreContextAction {

    @Getter
    protected final DataStoreEntryRef<T> ref;

    @Override
    protected boolean beforeExecute() throws Exception {
        ref.get().incrementBusyCounter();
        return true;
    }

    @Override
    protected void afterExecute() {
        ref.get().decrementBusyCounter();
    }

    @Override
    public String getShortcutName() {
        var name = DataStorage.get().getStoreEntryDisplayName(ref.get());
        return name + " (" + getDisplayName() + ")";
    }

    @SuppressWarnings("unchecked")
    public <V extends DataStore> StoreAction<V> asNeeded() {
        return (StoreAction<V>) this;
    }

    @SneakyThrows
    public StoreAction<T> withRef(DataStoreEntryRef<T> ref) {
        var node = toNode();
        node.set("ref", JacksonMapper.getDefault().valueToTree(ref));
        StoreAction<T> action = ActionJacksonMapper.parse(node);
        return action;
    }

    public Optional<StoreAction<T>> withConfigString(String configString) {
        try {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(configString);
            tree.set("ref", JacksonMapper.getDefault().valueToTree(ref));
            tree.put("id", getId());
            StoreAction<T> action = ActionJacksonMapper.parse(tree);
            return Optional.ofNullable(action);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<DataStoreEntry> getStoreEntryContext() {
        return List.of(ref.get());
    }
}
