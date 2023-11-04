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
import io.xpipe.app.util.FixedHierarchyStore;
import io.xpipe.core.store.*;
import io.xpipe.core.util.JacksonMapper;
import lombok.*;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Value
public class DataStoreEntry extends StorageElement {

    Map<String, Object> storeCache = new LinkedHashMap<>();
    @NonFinal
    Validity validity;
    @NonFinal
    @Setter
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
    @Getter
    @NonFinal
    DataStoreProvider provider;
    @NonFinal
    UUID categoryUuid;

    @NonFinal
    Object storePersistentState;

    @NonFinal
    JsonNode storePersistentStateNode;

    @NonFinal
    DataStoreColor color;

    @NonFinal
    @Setter
    Set<DataStoreEntry> childrenCache = null;

    private DataStoreEntry(
            Path directory, UUID uuid, UUID categoryUuid, String name, Instant lastUsed, Instant lastModified, JsonNode storeNode, boolean dirty,
            Validity validity, Configuration configuration, JsonNode storePersistentState, boolean expanded, DataStoreColor color
    ) {
        super(directory, uuid, name, lastUsed, lastModified, dirty);
        this.categoryUuid = categoryUuid;
        this.store = DataStorageParser.storeFromNode(storeNode);
        this.storeNode = storeNode;
        this.validity = validity;
        this.configuration = configuration;
        this.expanded = expanded;
        this.color = color;
        this.provider = store != null ? DataStoreProviders.byStoreClass(store.getClass()).orElse(null) : null;
        this.storePersistentStateNode = storePersistentState;
    }

    public static DataStoreEntry createNew(@NonNull String name, @NonNull DataStore store) {
        return createNew(UUID.randomUUID(), DataStorage.get().getSelectedCategory().getUuid(), name, store);
    }

    @SneakyThrows
    public static DataStoreEntry createNew(
            @NonNull UUID uuid, @NonNull UUID categoryUuid, @NonNull String name, @NonNull DataStore store
    ) {
        var entry = new DataStoreEntry(null, uuid, categoryUuid, name, Instant.now(), Instant.now(), DataStorageWriter.storeToNode(store), true,
                store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE, Configuration.defaultConfiguration(), null, false, null);
        return entry;
    }

    @SneakyThrows
    private static DataStoreEntry createExisting(
            @NonNull Path directory, @NonNull UUID uuid, @NonNull UUID categoryUuid, @NonNull String name, @NonNull Instant lastUsed,
            @NonNull Instant lastModified, JsonNode storeNode, Configuration configuration, JsonNode storePersistentState, boolean expanded,
            DataStoreColor color
    ) {
        return new DataStoreEntry(directory, uuid, categoryUuid, name, lastUsed, lastModified, storeNode, false, Validity.INCOMPLETE, configuration,
                storePersistentState, expanded, color);
    }

    public static DataStoreEntry fromDirectory(Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.getDefault();

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
        var categoryUuid = Optional.ofNullable(json.get("categoryUuid")).map(jsonNode -> UUID.fromString(jsonNode.textValue())).orElse(
                DataStorage.DEFAULT_CATEGORY_UUID);
        var name = json.required("name").textValue();

        var persistentState = stateJson.get("persistentState");
        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed")).map(jsonNode -> jsonNode.textValue()).map(Instant::parse).orElse(Instant.now());
        var lastModified = Optional.ofNullable(stateJson.get("lastModified")).map(jsonNode -> jsonNode.textValue()).map(Instant::parse).orElse(
                Instant.now());
        var configuration = Optional.ofNullable(json.get("configuration")).map(node -> {
            try {
                return mapper.treeToValue(node, Configuration.class);
            } catch (JsonProcessingException e) {
                return Configuration.defaultConfiguration();
            }
        }).orElse(Configuration.defaultConfiguration());
        var expanded = Optional.ofNullable(stateJson.get("expanded")).map(jsonNode -> jsonNode.booleanValue()).orElse(true);
        var color = Optional.ofNullable(stateJson.get("color")).map(node -> {
            try {
                return mapper.treeToValue(node, DataStoreColor.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).orElse(null);

        // Store loading is prone to errors.
        JsonNode storeNode = null;
        try {
            storeNode = mapper.readTree(storeFile.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        return createExisting(dir, uuid, categoryUuid, name, lastUsed, lastModified, storeNode, configuration, persistentState, expanded, color);
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof DataStoreEntry e && e.getUuid().equals(getUuid()));
    }

    @Override
    public String toString() {
        return getName();
    }

    public void setInRefresh(boolean newRefresh) {
        var changed = inRefresh != newRefresh;
        if (changed) {
            this.inRefresh = newRefresh;
            notifyUpdate();
        }
    }

    public <T extends DataStore> DataStoreEntryRef<T> ref() {
        return new DataStoreEntryRef<>(this);
    }

    public void setStoreCache(String key, Object value) {
        if (!Objects.equals(storeCache.put(key, value), value)) {
            notifyUpdate();
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends DataStoreState> T getStorePersistentState() {
        if (!(store instanceof StatefulDataStore<?> sds)) {
            return null;
        }

        if (storePersistentStateNode == null && storePersistentState == null) {
            storePersistentState = sds.createDefaultState();
            storePersistentStateNode = JacksonMapper.getDefault().valueToTree(storePersistentState);
        } else if (storePersistentState == null) {
            storePersistentState = JacksonMapper.getDefault().treeToValue(storePersistentStateNode, sds.getStateClass());
            if (storePersistentState == null) {
                storePersistentState = sds.createDefaultState();
                storePersistentStateNode = JacksonMapper.getDefault().valueToTree(storePersistentState);
            }
        }
        return (T) sds.getStateClass().cast(storePersistentState);
    }

    public void setStorePersistentState(Object value) {
        var changed = !Objects.equals(storePersistentState, value);
        this.storePersistentState = value;
        this.storePersistentStateNode = JacksonMapper.getDefault().valueToTree(value);
        if (changed) {
            this.dirty = true;
            notifyUpdate();
        }
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        this.dirty = true;
        notifyUpdate();
    }

    public void setCategoryUuid(UUID categoryUuid) {
        this.dirty = true;
        this.categoryUuid = categoryUuid;
        notifyUpdate();
    }

    @Override
    public Path[] getShareableFiles() {
        return new Path[]{
                directory.resolve("store.json"), directory.resolve("entry.json")
        };
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
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.set("color", mapper.valueToTree(color));
        stateObj.set("persistentState", storePersistentStateNode);
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

    public void setExpanded(boolean expanded) {
        var changed = expanded != this.expanded;
        this.expanded = expanded;
        if (changed) {
            dirty = true;
            notifyUpdate();
        }
    }

    public void setColor(DataStoreColor newColor) {
        var changed = !Objects.equals(color, newColor);
        this.color = newColor;
        if (changed) {
            dirty = true;
            notifyUpdate();
        }
    }

    public boolean isDisabled() {
        return validity == Validity.LOAD_FAILED;
    }

    public void applyChanges(DataStoreEntry e) {
        name = e.getName();
        storeNode = e.storeNode;
        store = e.store;
        validity = e.validity;
        lastModified = Instant.now();
        dirty = true;
        provider = e.provider;
        childrenCache = null;
        validity = store == null ? Validity.LOAD_FAILED : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        notifyUpdate();
    }

    public void setStoreInternal(DataStore store, boolean updateTime) {
        var changed = !Objects.equals(this.store, store);
        if (!changed) {
            return;
        }

        this.store = store;
        this.storeNode = DataStorageWriter.storeToNode(store);
        if (updateTime) {
            lastModified = Instant.now();
        }
        childrenCache = null;
        dirty = true;
    }

    public void validate() {
        try {
            validateOrThrow();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    public void validateOrThrow() throws Exception {
        try {
            store.checkComplete();
            setInRefresh(true);
            if (store instanceof ValidatableStore l) {
                l.validate();
            } else if (store instanceof FixedHierarchyStore h) {
                childrenCache = h.listChildren(this).stream().map(DataStoreEntryRef::get).collect(Collectors.toSet());
            }
        } finally {
            setInRefresh(false);
        }
    }

    public boolean tryMakeValid() {
        if (validity == Validity.LOAD_FAILED) {
            return false;
        }

        var complete = validity == Validity.COMPLETE;
        if (complete) {
            return false;
        }

        store = DataStorageParser.storeFromNode(storeNode);
        if (store == null) {
            validity = Validity.LOAD_FAILED;
            return false;
        }

        var newComplete = store.isComplete();
        if (!newComplete) {
            return false;
        }

        validity = Validity.COMPLETE;
        notifyUpdate();
        return true;
    }

    public boolean tryMakeInvalid() {
        if (validity == Validity.LOAD_FAILED) {
            return false;
        }

        if (validity == Validity.INCOMPLETE) {
            return false;
        }

        store = DataStorageParser.storeFromNode(storeNode);
        if (store == null) {
            validity = Validity.LOAD_FAILED;
            return false;
        }

        var newComplete = store.isComplete();
        if (newComplete) {
            return false;
        }

        validity = Validity.INCOMPLETE;
        notifyUpdate();
        return true;
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
        return getStore() != null;
    }

    public ObjectNode getResolvedNode() {
        if (store == null) {
            return null;
        }

        return JacksonMapper.getDefault().valueToTree(store);
    }

    @Getter
    public enum Validity {
        @JsonProperty("loadFailed") LOAD_FAILED(false),
        @JsonProperty("incomplete") INCOMPLETE(false),
        @JsonProperty("complete") COMPLETE(true);

        private final boolean isUsable;

        Validity(boolean isUsable) {
            this.isUsable = isUsable;
        }
    }
}
