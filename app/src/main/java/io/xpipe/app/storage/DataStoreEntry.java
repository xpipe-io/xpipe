package io.xpipe.app.storage;

import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.ext.UserScopeStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.*;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Value
public class DataStoreEntry extends StorageElement {

    Map<String, Object> storeCache = Collections.synchronizedMap(new HashMap<>());

    @NonFinal
    Validity validity;

    @NonFinal
    @Setter
    DataStorageNode storeNode;

    @Getter
    @NonFinal
    DataStore store;

    @NonFinal
    Configuration configuration;

    AtomicInteger busyCounter = new AtomicInteger();

    @Getter
    @NonFinal
    DataStoreProvider provider;

    @NonFinal
    UUID categoryUuid;

    @NonFinal
    DataStoreState storePersistentState;

    @NonFinal
    JsonNode storePersistentStateNode;

    @NonFinal
    @Setter
    Set<DataStoreEntry> childrenCache = null;

    @NonFinal
    String notes;

    @NonFinal
    String lastWrittenNotes;

    @NonFinal
    Order explicitOrder;

    @NonFinal
    String icon;

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            UUID categoryUuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            DataStore store,
            DataStorageNode storeNode,
            boolean dirty,
            Validity validity,
            Configuration configuration,
            JsonNode storePersistentState,
            boolean expanded,
            DataColor color,
            String notes,
            Order explicitOrder,
            String icon) {
        super(directory, uuid, name, lastUsed, lastModified, color, expanded, dirty);
        this.categoryUuid = categoryUuid;
        this.store = store;
        this.storeNode = storeNode;
        this.validity = validity;
        this.configuration = configuration;
        this.explicitOrder = explicitOrder;
        this.provider = store != null ? DataStoreProviders.byStore(store) : null;
        this.storePersistentStateNode = storePersistentState;
        this.notes = notes;
        this.icon = icon;
    }

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            UUID categoryUuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            DataStore store,
            Order explicitOrder,
            String icon) {
        super(directory, uuid, name, lastUsed, lastModified, null, false, false);
        this.categoryUuid = categoryUuid;
        this.store = store;
        this.explicitOrder = explicitOrder;
        this.icon = icon;
        this.storeNode = DataStorageNode.fail();
        this.validity = Validity.INCOMPLETE;
        this.configuration = Configuration.defaultConfiguration();
        this.expanded = false;
        this.provider = null;
        this.storePersistentStateNode = null;
    }

    public static DataStoreEntry createTempWrapper(@NonNull DataStore store) {
        return new DataStoreEntry(
                null,
                UUID.randomUUID(),
                DataStorage.get().getSelectedCategory().getUuid(),
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now(),
                store,
                null,
                null);
    }

    public static DataStoreEntry createNew(@NonNull String name, @NonNull DataStore store) {
        return createNew(
                UUID.randomUUID(), DataStorage.get().getSelectedCategory().getUuid(), name, store);
    }

    @SneakyThrows
    public static DataStoreEntry createNew(
            @NonNull UUID uuid, @NonNull UUID categoryUuid, @NonNull String name, @NonNull DataStore store) {
        var storeNode = DataStorageNode.ofNewStore(store);
        var storeFromNode = storeNode.parseStore();
        var validity = storeFromNode == null
                ? Validity.LOAD_FAILED
                : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        var entry = new DataStoreEntry(
                null,
                uuid,
                categoryUuid,
                name.trim(),
                Instant.now(),
                Instant.now(),
                storeFromNode,
                storeNode,
                true,
                validity,
                Configuration.defaultConfiguration(),
                null,
                false,
                null,
                null,
                null,
                null);
        return entry;
    }

    public String getEffectiveIconFile() {
        if (getValidity() == Validity.LOAD_FAILED) {
            return "disabled_icon.png";
        }

        if (icon == null) {
            return getProvider().getDisplayIconFileName(getStore());
        }

        return "icons/" + icon + ".svg";
    }

    public static Optional<DataStoreEntry> fromDirectory(Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.getDefault();

        var entryFile = dir.resolve("entry.json");
        var storeFile = dir.resolve("store.json");
        var stateFile = dir.resolve("state.json");
        var normalNotesFile = dir.resolve("notes.md");
        var encryptedNotesFile = dir.resolve("notes.json");
        if (!Files.exists(entryFile) || !Files.exists(storeFile)) {
            return Optional.empty();
        }

        if (!Files.exists(stateFile)) {
            stateFile = entryFile;
        }

        var json = mapper.readTree(entryFile.toFile());
        var stateJson = mapper.readTree(stateFile.toFile());
        var uuid = UUID.fromString(json.required("uuid").textValue());
        var categoryUuid = Optional.ofNullable(json.get("categoryUuid"))
                .map(jsonNode -> UUID.fromString(jsonNode.textValue()))
                .orElse(DataStorage.DEFAULT_CATEGORY_UUID);
        var name = json.required("name").textValue().trim();
        var color = Optional.ofNullable(json.get("color"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, DataColor.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .orElse(null);

        var iconNode = json.get("icon");
        String icon = iconNode != null && !iconNode.isNull() ? iconNode.asText() : null;

        var persistentState = stateJson.get("persistentState");
        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var lastModified = Optional.ofNullable(stateJson.get("lastModified"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var order = Optional.ofNullable(stateJson.get("order"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, Order.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .orElse(null);
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

        if (color == null) {
            color = Optional.ofNullable(stateJson.get("color"))
                    .map(node -> {
                        try {
                            return mapper.treeToValue(node, DataColor.class);
                        } catch (JsonProcessingException e) {
                            return null;
                        }
                    })
                    .orElse(null);
        }

        String notes = null;
        if (Files.exists(normalNotesFile)) {
            notes = Files.readString(normalNotesFile);
        }
        if (Files.exists(encryptedNotesFile)) {
            var node = DataStorageNode.readPossiblyEncryptedNode(mapper.readTree(encryptedNotesFile.toFile()));
            var mdNode = node.getContentNode().get("markdown");
            notes = mdNode != null ? mdNode.asText() : null;
        }
        if (notes != null && notes.isBlank()) {
            notes = null;
        }

        var fileNode = mapper.readTree(storeFile.toFile());
        var node = DataStorageNode.readPossiblyEncryptedNode(fileNode);
        var store = node.parseStore();
        return Optional.of(new DataStoreEntry(
                dir,
                uuid,
                categoryUuid,
                name,
                lastUsed,
                lastModified,
                store,
                node,
                false,
                store == null ? Validity.LOAD_FAILED : Validity.INCOMPLETE,
                configuration,
                persistentState,
                expanded,
                color,
                notes,
                order,
                icon));
    }

    public void setExplicitOrder(Order uuid) {
        var changed = !Objects.equals(explicitOrder, uuid);
        this.explicitOrder = uuid;
        if (changed) {
            notifyUpdate(false, true);
        }
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

    public boolean isPerUserStore() {
        var perUser = false;
        try {
            perUser = store instanceof UserScopeStore s && s.isPerUser();
        } catch (Exception ignored) {
        }
        return perUser;
    }

    public void incrementBusyCounter() {
        var r = busyCounter.incrementAndGet() == 1;
        if (r) {
            notifyUpdate(false, false);
        }
    }

    public boolean decrementBusyCounter() {
        var r = busyCounter.decrementAndGet() == 0;
        if (r) {
            notifyUpdate(false, false);
        }
        return r;
    }

    public <T extends DataStore> DataStoreEntryRef<T> ref() {
        return new DataStoreEntryRef<>(this);
    }

    public void setStoreCache(String key, Object value) {
        if (!Objects.equals(storeCache.put(key, value), value)) {
            notifyUpdate(false, false);
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
            storePersistentState =
                    JacksonMapper.getDefault().treeToValue(storePersistentStateNode, sds.getStateClass());
            if (storePersistentState == null) {
                storePersistentState = sds.createDefaultState();
                storePersistentStateNode = JacksonMapper.getDefault().valueToTree(storePersistentState);
            }
        }
        return (T) storePersistentState;
    }

    public void setIcon(String icon, boolean force) {
        if (this.icon != null && !force) {
            return;
        }

        var changed = !Objects.equals(this.icon, icon);
        this.icon = icon;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setStorePersistentState(DataStoreState value) {
        var changed = !Objects.equals(storePersistentState, value);
        this.storePersistentState = value;
        this.storePersistentStateNode = JacksonMapper.getDefault().valueToTree(value);
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        notifyUpdate(false, true);
    }

    public void setCategoryUuid(UUID categoryUuid) {
        var changed = !Objects.equals(this.categoryUuid, categoryUuid);
        this.categoryUuid = categoryUuid;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    @Override
    public Path[] getShareableFiles() {
        var notes = directory.resolve("notes.md");
        var list = List.of(directory.resolve("store.json"), directory.resolve("entry.json"));
        return Stream.concat(list.stream(), Files.exists(notes) ? Stream.of(notes) : Stream.of())
                .toArray(Path[]::new);
    }

    public void writeDataToDisk() throws Exception {
        if (!dirty) {
            return;
        }

        // Reset the dirty state early
        // That way, if any other changes are made during this save operation,
        // the dirty bit can be set to true again
        dirty = false;

        ObjectMapper mapper = JacksonMapper.getDefault();

        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("name", name);
        obj.put("categoryUuid", categoryUuid.toString());
        obj.set("color", mapper.valueToTree(color));
        obj.set("icon", mapper.valueToTree(icon));
        obj.set("configuration", mapper.valueToTree(configuration));

        ObjectNode stateObj = JsonNodeFactory.instance.objectNode();
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.set("persistentState", storePersistentStateNode);
        stateObj.put("expanded", expanded);
        stateObj.set("order", mapper.valueToTree(explicitOrder));

        var entryString = mapper.writeValueAsString(obj);
        var stateString = mapper.writeValueAsString(stateObj);
        var storeString = mapper.writeValueAsString(DataStorageNode.encryptNodeIfNeeded(storeNode));

        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("state.json"), stateString);
        Files.writeString(directory.resolve("entry.json"), entryString);
        Files.writeString(directory.resolve("store.json"), storeString);

        var encryptNotes = storeNode.isEncrypted();
        var normalNotesFile = directory.resolve("notes.md");
        var encryptedNotesFile = directory.resolve("notes.json");
        if (Files.exists(normalNotesFile) && (notes == null || encryptNotes)) {
            Files.delete(normalNotesFile);
        }
        if (Files.exists(encryptedNotesFile) && (notes == null || !encryptNotes)) {
            Files.delete(encryptedNotesFile);
        }
        if (notes != null && encryptNotes) {
            var notesNode = JsonNodeFactory.instance.objectNode();
            notesNode.put("markdown", notes);
            var storageNode = DataStorageNode.encryptNodeIfNeeded(new DataStorageNode(
                    notesNode, storeNode.isPerUser(), storeNode.isAvailableForUser(), storeNode.isEncrypted()));
            var string = mapper.writeValueAsString(storageNode);
            Files.writeString(encryptedNotesFile, string);
        } else if (notes != null) {
            Files.writeString(normalNotesFile, notes);
        }
        lastWrittenNotes = notes;
    }

    public void setNotes(String newNotes) {
        var changed = !Objects.equals(notes, newNotes);
        this.notes = newNotes;
        if (changed) {
            notifyUpdate(false, true);
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
        provider = e.provider;
        childrenCache = null;
        validity = store == null ? Validity.LOAD_FAILED : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        storePersistentState = e.storePersistentState;
        storePersistentStateNode = e.storePersistentStateNode;
        icon = e.icon;
        notifyUpdate(false, true);
    }

    public void setStoreInternal(DataStore store, boolean updateTime) {
        var changed = !Objects.equals(this.store, store);
        if (!changed) {
            return;
        }

        if (!storeNode.hasAccess()) {
            return;
        }

        this.store = store;
        this.storeNode = this.storeNode.withStore(store);
        this.provider = DataStoreProviders.byStore(store);
        if (updateTime) {
            lastModified = Instant.now();
        }
        childrenCache = null;
        dirty = true;
    }

    public void reassignStoreNode() {
        this.storeNode = this.storeNode.withStore(store);
        dirty = true;
    }

    public void validate() {
        try {
            validateOrThrow();
        } catch (Throwable ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    public void validateOrThrow() throws Throwable {
        if (store == null) {
            return;
        }

        if (!(store instanceof ValidatableStore l)) {
            return;
        }

        try {
            store.checkComplete();
            incrementBusyCounter();
            l.validate();
        } finally {
            decrementBusyCounter();
        }
    }

    public void refreshStore() {
        if (validity == Validity.LOAD_FAILED) {
            return;
        }

        DataStore newStore;
        try {
            newStore = storeNode.parseStore();
            // Check whether we have a provider as well
            DataStoreProviders.byStore(newStore);
        } catch (Throwable e) {
            ErrorEvent.fromThrowable(e).handle();
            newStore = null;
        }

        if (newStore == null) {
            var changed = store != null;
            store = null;
            validity = Validity.LOAD_FAILED;
            notifyUpdate(false, changed);
            return;
        }

        var newComplete = newStore.isComplete();
        if (!newComplete) {
            validity = Validity.INCOMPLETE;
            store = newStore;
            return;
        }

        var newPerUser = false;
        try {
            newPerUser = newStore instanceof UserScopeStore u && u.isPerUser();
        } catch (Exception ignored) {
        }
        var perUserChanged = isPerUserStore() != newPerUser;
        if (!newStore.equals(store)) {
            store = newStore;
        }
        validity = Validity.COMPLETE;
        notifyUpdate(false, perUserChanged);
    }

    public void initializeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                incrementBusyCounter();
                notifyUpdate(false, false);
                lifecycleStore.initializeStore();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                decrementBusyCounter();
                notifyUpdate(false, false);
            }
        }
    }

    public void finalizeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                incrementBusyCounter();
                notifyUpdate(false, false);
                lifecycleStore.finalizeStore();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            } finally {
                decrementBusyCounter();
                notifyUpdate(false, false);
            }
        }
    }

    public boolean finalizeEntryAsync() {
        if (store instanceof ExpandedLifecycleStore) {
            ThreadHelper.runAsync(() -> {
                finalizeEntry();
            });
            return true;
        } else {
            return false;
        }
    }

    public boolean shouldSave() {
        return getStore() != null;
    }

    @Getter
    public enum Validity {
        @JsonProperty("loadFailed")
        LOAD_FAILED(false),
        @JsonProperty("incomplete")
        INCOMPLETE(false),
        @JsonProperty("complete")
        COMPLETE(true);

        private final boolean isUsable;

        Validity(boolean isUsable) {
            this.isUsable = isUsable;
        }
    }

    @Getter
    public enum Order {
        @JsonProperty("top")
        TOP,
        @JsonProperty("bottom")
        BOTTOM
    }
}
