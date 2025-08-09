package io.xpipe.app.storage;

import io.xpipe.app.ext.*;
import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
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
    String icon;

    @NonFinal
    @Getter
    DataStoreColor color;

    @NonFinal
    @Getter
    boolean freeze;

    @NonFinal
    @Getter
    boolean pinToTop;

    @Getter
    @NonFinal
    int orderIndex;

    @Getter
    @NonFinal
    UUID breakOutCategory;

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
            JsonNode storePersistentState,
            boolean expanded,
            DataStoreColor color,
            String notes,
            String icon,
            boolean freeze,
            boolean pinToTop,
            int orderIndex,
            UUID breakOutCategory) {
        super(directory, uuid, name, lastUsed, lastModified, expanded, dirty);
        this.color = color;
        this.categoryUuid = categoryUuid;
        this.store = store;
        this.storeNode = storeNode;
        this.provider =
                store != null ? DataStoreProviders.byStoreIfPresent(store).orElse(null) : null;
        this.validity = this.provider != null ? validity : Validity.LOAD_FAILED;
        this.storePersistentStateNode = storePersistentState;
        this.notes = notes;
        this.icon = icon;
        this.freeze = freeze;
        this.pinToTop = pinToTop;
        this.orderIndex = orderIndex;
        this.breakOutCategory = breakOutCategory;
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
                DataStorageNode.fail(),
                false,
                Validity.COMPLETE,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                0,
                null);
    }

    public static DataStoreEntry createNew(@NonNull NameableStore store) {
        return createNew(
                UUID.randomUUID(), DataStorage.get().getSelectedCategory().getUuid(), store.getName(), store);
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
                name.strip(),
                Instant.now(),
                Instant.now(),
                storeFromNode,
                storeNode,
                true,
                validity,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                0,
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

        var found = SystemIconManager.getIcon(icon);
        if (found.isPresent()) {
            return SystemIconManager.getIconFile(found.get());
        } else {
            return "disabled_icon.png";
        }
    }

    public static Optional<DataStoreEntry> fromDirectory(Path dir) throws IOException {
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
        var breakOutCategory = Optional.ofNullable(json.get("breakOutCategoryUuid"))
                .filter(jsonNode -> !jsonNode.isNull())
                .map(jsonNode -> UUID.fromString(jsonNode.asText()))
                .orElse(null);
        var name = json.required("name").textValue().strip();

        // Fix for legacy issue where entries could have empty names
        if (name.isBlank()) {
            return Optional.empty();
        }

        var color = Optional.ofNullable(json.get("color"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, DataStoreColor.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .orElse(null);
        var freeze = Optional.ofNullable(json.get("freeze"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(false);
        var pinToTop = Optional.ofNullable(json.get("pinToTop"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(false);

        var iconNode = json.get("icon");
        String icon = iconNode != null && !iconNode.isNull() ? iconNode.asText() : null;

        // Legacy compat for old icons
        if (icon != null && !icon.contains("/")) {
            icon = "selfhst/" + icon;
        }

        var persistentState = stateJson.get("persistentState");
        var orderIndex = Optional.ofNullable(json.get("orderIndex"))
                .map(jsonNode -> jsonNode.intValue())
                .orElse(0);
        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var lastModified = Optional.ofNullable(stateJson.get("lastModified"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var expanded = Optional.ofNullable(stateJson.get("expanded"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(true);

        if (color == null) {
            color = Optional.ofNullable(stateJson.get("color"))
                    .map(node -> {
                        try {
                            return mapper.treeToValue(node, DataStoreColor.class);
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

        DataStorageNode node;
        try {
            var fileNode = mapper.readTree(storeFile.toFile());
            node = DataStorageNode.readPossiblyEncryptedNode(fileNode);
        } catch (JacksonException ex) {
            ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
            node = DataStorageNode.fail();
        }

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
                persistentState,
                expanded,
                color,
                notes,
                icon,
                freeze,
                pinToTop,
                orderIndex,
                breakOutCategory));
    }

    public void setColor(DataStoreColor newColor) {
        var changed = !Objects.equals(color, newColor);
        this.color = newColor;
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

    public boolean isChangedForReload(DataStoreEntry other) {
        return !Objects.equals(getStore(), other.getStore())
                || !Objects.equals(getName(), other.getName())
                || !Objects.equals(getNotes(), other.getNotes())
                || !Objects.equals(getColor(), other.getColor())
                || !Objects.equals(getCategoryUuid(), other.getCategoryUuid())
                || !Objects.equals(getOrderIndex(), other.getOrderIndex())
                || !Objects.equals(getEffectiveIconFile(), other.getEffectiveIconFile());
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

        if (storePersistentStateNode != null && storePersistentStateNode.isNull()) {
            storePersistentStateNode = null;
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

    public void setBreakOutCategory(DataStoreCategory category) {
        var changed = !Objects.equals(breakOutCategory, category != null ? category.getUuid() : null);
        this.breakOutCategory = category != null ? category.getUuid() : null;
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

    public void setOrderIndex(int orderIndex) {
        var changed = this.orderIndex != orderIndex;
        this.orderIndex = orderIndex;
        if (changed) {
            notifyUpdate(false, false);
        }
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
        obj.put("breakOutCategoryUuid", breakOutCategory != null ? breakOutCategory.toString() : null);
        obj.set("color", mapper.valueToTree(color));
        obj.set("icon", mapper.valueToTree(icon));
        obj.put("freeze", freeze);
        obj.put("pinToTop", pinToTop);
        obj.put("orderIndex", orderIndex);

        ObjectNode stateObj = JsonNodeFactory.instance.objectNode();
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.set("persistentState", storePersistentStateNode);
        stateObj.put("expanded", expanded);

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
                    notesNode, storeNode.isPerUser(), storeNode.isReadableForUser(), storeNode.isEncrypted()));
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

    public void setFreeze(boolean newValue) {
        var changed = freeze != newValue;
        this.freeze = newValue;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setPinToTop(boolean newValue) {
        var changed = pinToTop != newValue;
        this.pinToTop = newValue;
        if (changed) {
            notifyUpdate(false, false);
            dirty = true;
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
        storeCache.clear();
        storeCache.putAll(e.storeCache);
        validity = store == null ? Validity.LOAD_FAILED : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        storePersistentState = e.storePersistentState;
        storePersistentStateNode = e.storePersistentStateNode;
        icon = e.icon;
        categoryUuid = e.categoryUuid;
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
        this.storeNode = DataStorageNode.ofNewStore(store);
        this.provider = DataStoreProviders.byStore(store);
        this.validity = provider != null
                ? (store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE)
                : Validity.LOAD_FAILED;
        if (updateTime) {
            lastModified = Instant.now();
        }
        childrenCache = null;
        dirty = true;
        notifyUpdate(false, updateTime);
    }

    public void reassignStoreNode() {
        this.storeNode = DataStorageNode.ofNewStore(store);
        dirty = true;
    }

    public void validate() {
        try {
            validateOrThrow();
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
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
            ErrorEventFactory.fromThrowable(e).handle();
            newStore = null;
        }

        if (newStore == null) {
            var changed = store != null;
            store = null;
            validity = Validity.LOAD_FAILED;
            if (changed) {
                notifyUpdate(false, false);
            }
            return;
        }

        var storeChanged = !Objects.equals(store, newStore);
        var newComplete = newStore.isComplete();
        if (!newComplete) {
            validity = Validity.INCOMPLETE;
            store = newStore;
            if (storeChanged) {
                notifyUpdate(false, false);
            }
            return;
        }

        var newPerUser = false;
        try {
            newPerUser = newStore instanceof UserScopeStore u && u.isPerUser();
        } catch (Exception ignored) {
        }
        var perUserChanged = isPerUserStore() != newPerUser;
        if (storeChanged) {
            store = newStore;
        }
        validity = Validity.COMPLETE;
        if (storeChanged || perUserChanged) {
            notifyUpdate(false, false);
        }
    }

    public void initializeEntry() {
        if (store instanceof ExpandedLifecycleStore lifecycleStore) {
            try {
                incrementBusyCounter();
                notifyUpdate(false, false);
                lifecycleStore.initializeStore();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).handle();
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
                ErrorEventFactory.fromThrowable(e).handle();
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
}
