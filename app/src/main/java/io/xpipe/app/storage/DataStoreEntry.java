package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FixedHierarchyStore;
import io.xpipe.core.util.JacksonMapper;
import lombok.*;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.lang.Integer.MAX_VALUE;

@Value
@EqualsAndHashCode(callSuper = true)
public class DataStoreEntry extends StorageElement {

    @NonFinal
    State state;

    @NonFinal
    String information;

    @NonFinal
    JsonNode storeNode;

    @NonFinal
    DataStore store;

    @NonFinal
    Configuration configuration;

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            String information,
            JsonNode storeNode,
            boolean dirty,
            State state,
            Configuration configuration)
            throws Exception {
        super(directory, uuid, name, lastUsed, lastModified, dirty);
        this.information = information;
        this.store = DataStorageParser.storeFromNode(storeNode);
        this.storeNode = storeNode;
        this.state = state;
        this.configuration = configuration;
    }

    @SneakyThrows
    public static DataStoreEntry createNew(@NonNull UUID uuid, @NonNull String name, @NonNull DataStore store) {
        var entry = new DataStoreEntry(
                null,
                uuid,
                name,
                Instant.now(),
                Instant.now(),
                null,
                DataStorageWriter.storeToNode(store),
                true,
                State.LOAD_FAILED,
                Configuration.defaultConfiguration());
        entry.refresh(false);
        return entry;
    }

    @SneakyThrows
    private static DataStoreEntry createExisting(
            @NonNull Path directory,
            @NonNull UUID uuid,
            @NonNull String name,
            @NonNull Instant lastUsed,
            @NonNull Instant lastModified,
            String information,
            JsonNode storeNode,
            State state,
            Configuration configuration) {
        var entry = new DataStoreEntry(
                directory, uuid, name, lastUsed, lastModified, information, storeNode, false, state, configuration);
        return entry;
    }

    public static DataStoreEntry fromDirectory(Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.newMapper();

        var entryFile = dir.resolve("entry.json");
        var storeFile = dir.resolve("store.json");
        if (!Files.exists(entryFile) || !Files.exists(storeFile)) {
            return null;
        }

        var json = mapper.readTree(entryFile.toFile());
        var uuid = UUID.fromString(json.required("uuid").textValue());
        var name = json.required("name").textValue();
        var state = Optional.ofNullable(json.get("state"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, State.class);
                    } catch (JsonProcessingException e) {
                        return State.INCOMPLETE;
                    }
                })
                .orElse(State.INCOMPLETE);
        var information = Optional.ofNullable(json.get("information"))
                .map(JsonNode::textValue)
                .orElse(null);

        var lastUsed = Instant.parse(json.required("lastUsed").textValue());
        var lastModified = Instant.parse(json.required("lastModified").textValue());
        var configuration = Optional.ofNullable(json.get("configuration"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, Configuration.class);
                    } catch (JsonProcessingException e) {
                        return Configuration.defaultConfiguration();
                    }
                })
                .orElse(Configuration.defaultConfiguration());

        // Store loading is prone to errors.
        JsonNode storeNode = null;
        try {
            storeNode = mapper.readTree(storeFile.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        return createExisting(dir, uuid, name, lastUsed, lastModified, information, storeNode, state, configuration);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        simpleRefresh();
    }

    public DataStore getStore() {
        return store;
    }

    public boolean matches(String filter) {
        return getName().toLowerCase().contains(filter.toLowerCase())
                || (!isDisabled()
                                && (getProvider().toSummaryString(getStore(), MAX_VALUE) != null
                                        && getProvider()
                                                .toSummaryString(getStore(), MAX_VALUE)
                                                .toLowerCase()
                                                .contains(filter.toLowerCase()))
                        || (information != null && information.toLowerCase().contains(filter.toLowerCase())));
    }

    public boolean isDisabled() {
        return state == State.LOAD_FAILED;
    }

    public void applyChanges(DataStoreEntry e) {
        name = e.getName();
        storeNode = e.storeNode;
        store = e.store;
        state = e.state;
        lastModified = Instant.now();
        information = e.information;
        dirty = true;
        simpleRefresh();
    }

    /*
    TODO: Implement singular change functions
     */
    public void refresh(boolean deep) throws Exception {
        // Assume that refresh can't be called while validating.
        // Therefore the validation must be stuck if that happens
        if (state == State.VALIDATING) {
            state = State.COMPLETE_BUT_INVALID;
        }

        var oldStore = store;
        DataStore newStore = DataStorageParser.storeFromNode(storeNode);
        if (newStore == null
                || DataStoreProviders.byStoreClass(store.getClass()).isEmpty()) {
            store = null;
            state = State.LOAD_FAILED;
            information = null;
            dirty = dirty || oldStore != null;
            listeners.forEach(l -> l.onUpdate());
        } else {
            var newNode = DataStorageWriter.storeToNode(newStore);
            var nodesEqual = Objects.equals(storeNode, newNode);
            if (!nodesEqual) {
                storeNode = newNode;
            }

            dirty = dirty || !nodesEqual;
            store = newStore;
            var complete = newStore.isComplete();

            try {
                if (complete && !newStore.shouldPersist()) {
                    DataStorage.get().deleteStoreEntry(this);
                    return;
                }

                if (complete && deep) {
                    state = State.VALIDATING;
                    listeners.forEach(l -> l.onUpdate());
                    store.validate();

                    if (store instanceof FixedHierarchyStore) {
                        DataStorage.get().refreshChildren(this);
                    }

                    state = State.COMPLETE_AND_VALID;
                    information = getProvider().queryInformationString(getStore(), 50);
                    dirty = true;
                } else if (complete) {
                    var stateToUse = state == State.LOAD_FAILED || state == State.INCOMPLETE
                            ? State.COMPLETE_BUT_INVALID
                            : state;
                    state = stateToUse;
                } else {
                    var stateToUse = state == State.LOAD_FAILED ? State.COMPLETE_BUT_INVALID : State.INCOMPLETE;
                    state = stateToUse;
                }
            } catch (Exception e) {
                state = store.isComplete() ? State.COMPLETE_BUT_INVALID : State.INCOMPLETE;
                information = null;
                throw e;
            } finally {
                propagateUpdate();
            }
        }
    }

    @Override
    protected boolean shouldSave() {
        return getStore() == null || getStore().shouldSave();
    }

    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public void writeDataToDisk() throws Exception {
        if (!dirty) {
            return;
        }

        ObjectMapper mapper = JacksonMapper.newMapper();
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("name", name);
        obj.put("information", information);
        obj.put("lastUsed", lastUsed.toString());
        obj.put("lastModified", lastModified.toString());
        obj.set("state", mapper.valueToTree(state));
        obj.set("configuration", mapper.valueToTree(configuration));

        var entryString = mapper.writeValueAsString(obj);
        var storeString = mapper.writeValueAsString(storeNode);

        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("entry.json"), entryString);
        Files.writeString(directory.resolve("store.json"), storeString);
        dirty = false;
    }

    public ObjectNode getResolvedNode() {
        if (store == null) {
            return null;
        }

        return JacksonMapper.getDefault().valueToTree(store);
    }

    public DataStoreProvider getProvider() {
        if (store == null) {
            return null;
        }

        return DataStoreProviders.byStoreClass(store.getClass()).orElse(null);
    }

    @Getter
    public static enum State {
        @JsonProperty("loadFailed")
        LOAD_FAILED(false),
        @JsonProperty("incomplete")
        INCOMPLETE(false),
        @JsonProperty("completeButInvalid")
        COMPLETE_BUT_INVALID(true),
        @JsonProperty("validating")
        VALIDATING(true),
        @JsonProperty("completeAndValid")
        COMPLETE_AND_VALID(true);

        private final boolean isUsable;

        State(boolean isUsable) {
            this.isUsable = isUsable;
        }
    }
}
