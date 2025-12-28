package io.xpipe.app.hub.action;

import io.xpipe.app.action.*;
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
@Getter
public final class BatchStoreAction<T extends DataStore> extends SerializableAction implements StoreContextAction {

    private final List<StoreAction<T>> actions;

    @Override
    public ActionProvider getProvider() {
        return actions.getFirst().getProvider();
    }

    @Override
    public String getShortcutName() {
        var names = actions.size() > 3
                ? actions.size() + " connections"
                : actions.stream()
                        .map(a -> DataStorage.get()
                                .getStoreEntryDisplayName(a.getRef().get()))
                        .collect(Collectors.joining(", "));
        return names + " (" + getDisplayName() + ")";
    }

    @Override
    public void executeImpl() {
        for (AbstractAction action : actions) {
            if (!action.executeSyncImpl(true)) {
                break;
            }
        }
    }

    @Override
    public boolean isMutation() {
        return actions.stream().anyMatch(StoreAction::isMutation);
    }

    @Override
    public boolean forceConfirmation() {
        return actions.stream().anyMatch(StoreAction::forceConfirmation);
    }

    @SneakyThrows
    public BatchStoreAction<T> withRefs(List<DataStoreEntryRef<T>> refs) {
        var node = toNode();
        node.set("ref", JacksonMapper.getDefault().valueToTree(refs));
        BatchStoreAction<T> action = ActionJacksonMapper.parse(node);
        return action;
    }

    public List<DataStoreEntryRef<T>> getRefs() {
        return actions.stream().map(action -> action.getRef()).collect(Collectors.toList());
    }

    public Optional<BatchStoreAction<?>> withConfigString(String configString) {
        try {
            var tree = (ObjectNode) JacksonMapper.getDefault().readTree(configString);
            tree.set("ref", JacksonMapper.getDefault().valueToTree(getRefs()));
            tree.put("id", getId());
            BatchStoreAction<?> action = ActionJacksonMapper.parse(tree);
            return Optional.ofNullable(action);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<DataStoreEntry> getStoreEntryContext() {
        return getRefs().stream().map(DataStoreEntryRef::get).toList();
    }
}
