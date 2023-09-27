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
import java.util.*;

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

    @Getter
    @NonFinal
    DataStore store;

    @NonFinal
    Configuration configuration;

    @NonFinal
    boolean expanded;

    @NonFinal
    boolean inRefresh;

    @NonFinal
    @Setter
    boolean observing;

    @NonFinal
    DataStoreProvider provider;

   Map<String, Object> elementState = new LinkedHashMap<>();

    @NonFinal
    UUID categoryUuid;

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            UUID categoryUuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            String information,
            JsonNode storeNode,
            boolean dirty,
            State state,
            Configuration configuration,
            boolean expanded) {
        super(directory, uuid,  name, lastUsed, lastModified, dirty);
        this.categoryUuid = categoryUuid;
        this.information = information;
        this.store = DataStorageParser.storeFromNode(storeNode);
        this.storeNode = storeNode;
        this.state = state;
        this.configuration = configuration;
        this.expanded = expanded;
        this.provider = store != null ? DataStoreProviders.byStoreClass(store.getClass()).orElse(null) : null;
    }

    public void setInRefresh(boolean newRefresh) {
        var changed = inRefresh != newRefresh;
        if (changed) {
            this.inRefresh = newRefresh;
            notifyUpdate();
        }
    }

    @SneakyThrows
    public static DataStoreEntry createNew(@NonNull UUID uuid, @NonNull UUID categoryUuid, @NonNull String name, @NonNull DataStore store) {
        var entry = new DataStoreEntry(
                null,
                uuid,
                categoryUuid,
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
            @NonNull UUID categoryUuid,
            @NonNull String name,
            @NonNull Instant lastUsed,
            @NonNull Instant lastModified,
            String information,
            JsonNode storeNode,
            State state,
            Configuration configuration,
            boolean expanded) {
        return new DataStoreEntry(
                directory,
                uuid,
                categoryUuid,
                name,
                lastUsed,
                lastModified,
                information,
                storeNode,
                false,
                state,
                configuration,
                expanded);
    }

    public static DataStoreEntry fromDirectory(Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.newMapper();

        var entryFile = dir.resolve("entry.json");
        var storeFile = dir.resolve("store.json");
        var stateFile = dir.resolve("state.json");
        if (!Files.exists(entryFile) || !Files.exists(storeFile)) {
            return null;
        }

        if (!Files.exists(stateFile)) {
            stateFile = entryFile;
        }

        var json = mapper.readTree(entryFile.toFile());
        var stateJson = mapper.readTree(stateFile.toFile());
        var uuid = UUID.fromString(json.required("uuid").textValue());
        var categoryUuid = Optional.ofNullable(json.get("categoryUuid")).map(jsonNode -> UUID.fromString(jsonNode.textValue())).orElse(DataStorage.DEFAULT_CATEGORY_UUID);
        var name = json.required("name").textValue();
        var state = Optional.ofNullable(stateJson.get("state"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, State.class);
                    } catch (JsonProcessingException e) {
                        return State.INCOMPLETE;
                    }
                })
                .orElse(State.INCOMPLETE);
        var information = Optional.ofNullable(stateJson.get("information"))
                .map(JsonNode::textValue)
                .orElse(null);

        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed")).map(jsonNode -> jsonNode.textValue()).map(Instant::parse).orElse(Instant.now());
        var lastModified = Optional.ofNullable(stateJson.get("lastModified")).map(jsonNode -> jsonNode.textValue()).map(Instant::parse).orElse(Instant.now());
        var configuration = Optional.ofNullable(json.get("configuration"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, Configuration.class);
                    } catch (JsonProcessingException e) {
                        return Configuration.defaultConfiguration();
                    }
                })
                .orElse(Configuration.defaultConfiguration());
        var expanded = Optional.ofNullable(stateJson.get("expanded"))
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
                dir, uuid, categoryUuid, name, lastUsed, lastModified, information, storeNode, state, configuration, expanded);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        this.dirty = true;
        simpleRefresh();
    }

    public void setCategoryUuid(UUID categoryUuid) {
        this.dirty = true;
        this.categoryUuid = categoryUuid;
        notifyUpdate();
    }

    @Override
    public Path[] getShareableFiles() {
        return new Path[] {directory.resolve("store.json"), directory.resolve("entry.json")};
    }

    public void setExpanded(boolean expanded) {
        var changed = expanded != this.expanded;
        this.expanded = expanded;
        if (changed) {
            dirty = true;
            notifyUpdate();
        }
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
            notifyUpdate();
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
                    inRefresh = true;
                    notifyUpdate();
                    store.validate();
                    inRefresh = false;
                    state = State.COMPLETE_AND_VALID;
                    information = getProvider().queryInformationString(getStore(), 50);
                    dirty = true;
                } else if (complete) {
                    state = state == State.LOAD_FAILED || state == State.INCOMPLETE
                            ? State.COMPLETE_NOT_VALIDATED
                            : state;
                    information = state == State.COMPLETE_AND_VALID
                            ? information
                            : state == State.COMPLETE_BUT_INVALID
                                    ? getProvider().queryInvalidInformationString(getStore(), 50)
                                    : null;
                } else {
                    state = state == State.LOAD_FAILED ? State.COMPLETE_BUT_INVALID : State.INCOMPLETE;
                }
            } catch (Exception e) {
                inRefresh = false;
                state = State.COMPLETE_BUT_INVALID;
                information = getProvider().queryInvalidInformationString(getStore(), 50);
                throw e;
            } finally {
                notifyUpdate();
            }
        }
    }

    @SneakyThrows
    public void initializeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                inRefresh = true;
                notifyUpdate();
                lifecycleStore.initializeValidate();
                inRefresh = false;
            } catch (Exception e) {
                inRefresh = false;
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                notifyUpdate();
            }
        }
    }

    @SneakyThrows
    public void finalizeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                inRefresh = true;
                notifyUpdate();
                lifecycleStore.finalizeValidate();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                notifyUpdate();
            }
        }
    }

    public boolean shouldSave() {
        return getStore() == null || getStore().shouldSave();
    }

    public void simpleRefresh() {
        try {
            refresh(false);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public void writeDataToDisk() throws Exception {
        if (!dirty) {
            return;
        }

        ObjectMapper mapper = JacksonMapper.getDefault();
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        ObjectNode stateObj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("name", name);
        obj.put("categoryUuid", categoryUuid.toString());
        stateObj.put("information", information);
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.set("state", mapper.valueToTree(state));
        obj.set("configuration", mapper.valueToTree(configuration));
        stateObj.put("expanded", expanded);

        var entryString = mapper.writeValueAsString(obj);
        var stateString = mapper.writeValueAsString(stateObj);
        var storeString = mapper.writeValueAsString(storeNode);

        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("state.json"), stateString);
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
        @JsonProperty("completeAndValid")
        COMPLETE_AND_VALID(true);

        private final boolean isUsable;

        State(boolean isUsable) {
            this.isUsable = isUsable;
        }
    }
}
