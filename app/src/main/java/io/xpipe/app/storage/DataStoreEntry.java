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
import io.xpipe.core.store.ExpandedLifecycleStore;
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

    @NonFinal
    boolean expanded;

    @NonFinal
    DataStoreProvider provider;

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
            Configuration configuration,
            boolean expanded) {
        super(directory, uuid, name, lastUsed, lastModified, dirty);
        this.information = information;
        this.store = DataStorageParser.storeFromNode(storeNode);
        this.storeNode = storeNode;
        this.state = state;
        this.configuration = configuration;
        this.expanded = expanded;
        this.provider = store != null ? DataStoreProviders.byStoreClass(store.getClass()).orElse(null) : null;
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
                Configuration.defaultConfiguration(),
                false);
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
            Configuration configuration,
            boolean expanded) {

        // The validation must be stuck if that happens
        var stateToUse = state;
        if (state == State.VALIDATING) {
            stateToUse = State.COMPLETE_BUT_INVALID;
        }

        var entry = new DataStoreEntry(
                directory,
                uuid,
                name,
                lastUsed,
                lastModified,
                information,
                storeNode,
                false,
                stateToUse,
                configuration,
                expanded);
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
        var expanded = Optional.ofNullable(json.get("expanded"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(true);

        // Store loading is prone to errors.
        JsonNode storeNode = null;
        try {
            storeNode = mapper.readTree(storeFile.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        return createExisting(
                dir, uuid, name, lastUsed, lastModified, information, storeNode, state, configuration, expanded);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        simpleRefresh();
    }

    public void setExpanded(boolean expanded) {
        this.dirty = true;
        this.expanded = expanded;
        notifyListeners();
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
        provider = e.provider;
        simpleRefresh();
    }

    void setStoreInternal(DataStore store, boolean updateTime) {
        this.store = store;
        this.storeNode = DataStorageWriter.storeToNode(store);
        if (updateTime) {
            lastModified = Instant.now();
        }
        dirty = true;
    }

    /*
    TODO: Implement singular change functions
     */
    public void refresh(boolean deep) throws Exception {
        var oldStore = store;
        DataStore newStore = DataStorageParser.storeFromNode(storeNode);
        if (newStore == null
                || DataStoreProviders.byStoreClass(store.getClass()).isEmpty()) {
            store = null;
            state = State.LOAD_FAILED;
            information = null;
            provider = null;
            dirty = dirty || oldStore != null;
            notifyListeners();
        } else {
            var newNode = DataStorageWriter.storeToNode(newStore);
            var nodesEqual = Objects.equals(storeNode, newNode);
            if (!nodesEqual) {
                storeNode = newNode;
            }

            dirty = dirty || !nodesEqual;
            store = newStore;
            provider = DataStoreProviders.byStoreClass(newStore.getClass()).orElse(null);
            var complete = newStore.isComplete();

            try {
                if (complete && !newStore.shouldPersist()) {
                    DataStorage.get().deleteStoreEntry(this);
                    return;
                }

                if (complete && deep) {
                    state = State.VALIDATING;
                    notifyListeners();
                    store.validate();
                    state = State.COMPLETE_AND_VALID;
                    information = getProvider().queryInformationString(getStore(), 50);
                    dirty = true;
                } else if (complete) {
                    var stateToUse = state == State.LOAD_FAILED || state == State.INCOMPLETE
                            ? State.COMPLETE_NOT_VALIDATED
                            : state;
                    state = stateToUse;
                    information = state == State.COMPLETE_AND_VALID
                            ? information
                            : state == State.COMPLETE_BUT_INVALID
                                    ? getProvider().queryInvalidInformationString(getStore(), 50)
                                    : null;
                } else {
                    var stateToUse = state == State.LOAD_FAILED ? State.COMPLETE_BUT_INVALID : State.INCOMPLETE;
                    state = stateToUse;
                }
            } catch (Exception e) {
                state = State.COMPLETE_BUT_INVALID;
                information = getProvider().queryInvalidInformationString(getStore(), 50);
                throw e;
            } finally {
                notifyListeners();
            }
        }
    }

    @SneakyThrows
    public void initializeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                state = State.VALIDATING;
                notifyListeners();
                lifecycleStore.initializeValidate();
                state = State.COMPLETE_AND_VALID;
            } catch (Exception e) {
                state = State.COMPLETE_BUT_INVALID;
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                notifyListeners();
            }
        }
    }

    @SneakyThrows
    public void finalizeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                state = State.VALIDATING;
                notifyListeners();
                lifecycleStore.finalizeValidate();
                state = State.COMPLETE_AND_VALID;
            } catch (Exception e) {
                state = State.COMPLETE_BUT_INVALID;
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                notifyListeners();
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
        obj.put("expanded", expanded);

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
        return provider;
    }

    @Getter
    public enum State {
        @JsonProperty("loadFailed")
        LOAD_FAILED(false),
        @JsonProperty("incomplete")
        INCOMPLETE(false),
        @JsonProperty("completeNotValidated")
        COMPLETE_NOT_VALIDATED(true),
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
