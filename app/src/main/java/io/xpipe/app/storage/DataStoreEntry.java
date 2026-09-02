package io.xpipe.app.storage;

import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.store.*;
import io.xpipe.app.util.JacksonMapper;
import io.xpipe.app.util.ThreadHelper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class DataStoreEntry extends DataStorageElement {

    Map<String, Object> storeCache = Collections.synchronizedMap(new HashMap<>());
    AtomicInteger busyCounter = new AtomicInteger();

    @NonFinal
    Validity validity;

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
    @Getter
    DataStoreColor color;

    @NonFinal
    @Getter
    boolean template;

    @NonFinal
    @Getter
    boolean pinToTop;

    @Getter
    @NonFinal
    UUID breakOutCategory;

    List<String> tags;

    @NonFinal
    DataStoreEntryNode<DataStore> storeNode;

    @NonFinal
    DataStoreEntryNode<JsonNode> stateNode;

    @NonFinal
    DataStoreEntryNode<JsonNode> entryNode;

    @NonFinal
    DataStoreEntryNode<String> notesNode;

    private DataStoreEntry(
            Path directory,
            UUID uuid,
            UUID categoryUuid,
            String name,
            Instant created,
            Instant lastUsed,
            Instant lastModified,
            boolean dirty,
            Validity validity,
            JsonNode storePersistentState,
            boolean expanded,
            DataStoreColor color,
            String icon,
            double orderIndex,
            boolean template,
            boolean pinToTop,
            UUID breakOutCategory,
            List<String> tags,
            DataStoreEntryNode<DataStore> storeNode,
            DataStoreEntryNode<JsonNode> stateNode,
            DataStoreEntryNode<JsonNode> entryNode,
            DataStoreEntryNode<String> notesNode) {
        super(directory, uuid, name, created, lastUsed, lastModified, expanded, dirty, icon, orderIndex);
        this.color = color;
        this.categoryUuid = categoryUuid;
        this.storeNode = storeNode;
        this.stateNode = stateNode;
        this.entryNode = entryNode;
        this.notesNode = notesNode;
        this.provider = storeNode != null && storeNode.isAccessible()
                ? DataStoreProvider.byStoreIfPresent(storeNode.getValue()).orElse(null)
                : null;
        this.validity = this.provider != null ? validity : Validity.LOAD_FAILED;
        this.storePersistentStateNode = storePersistentState;
        this.template = template;
        this.pinToTop = pinToTop;
        this.breakOutCategory = breakOutCategory;
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof DataStoreEntry e && e.getUuid().equals(getUuid()));
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

    public static DataStoreEntry createTempWrapper(@NonNull DataStore store) {
        var storage = DataStorage.get();
        var cat = storage != null ? storage.getSelectedCategory().getUuid() : UUID.randomUUID();
        return new DataStoreEntry(
                null,
                UUID.randomUUID(),
                cat,
                UUID.randomUUID().toString(),
                Instant.now(),
                Instant.now(),
                Instant.now(),
                false,
                Validity.COMPLETE,
                null,
                false,
                null,
                null,
                0.0,
                false,
                false,
                null,
                new ArrayList<>(),
                DataStoreEntryNode.of(store),
                null,
                null,
                null);
    }

    public static DataStoreEntry createNew(@NonNull NameableStore store) {
        return createNew(
                UUID.randomUUID(), DataStorage.get().getSelectedCategory().getUuid(), store.getName(), store);
    }

    public static DataStoreEntry createNew(@NonNull String name, DataStore store) {
        return createNew(
                UUID.randomUUID(), DataStorage.get().getSelectedCategory().getUuid(), name, store);
    }

    @SneakyThrows
    public static DataStoreEntry createNew(
            @NonNull UUID uuid, @NonNull UUID categoryUuid, @NonNull String name, DataStore store) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Name is empty");
        }

        var storeNode = DataStoreEntryNode.of(store);
        var validity =
                store == null ? Validity.LOAD_FAILED : store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        var now = Instant.now();
        var entry = new DataStoreEntry(
                null,
                uuid,
                categoryUuid,
                name.strip(),
                now,
                now,
                now,
                true,
                validity,
                null,
                false,
                null,
                null,
                0.0,
                false,
                false,
                null,
                new ArrayList<>(),
                storeNode,
                null,
                null,
                null);
        return entry;
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

        DataStoreEntryNode<JsonNode> entryNode = DataStoreEntryNode.parse(entryFile, JsonNode.class);
        if (entryNode == null) {
            return Optional.empty();
        }

        if (!entryNode.isAccessible()) {
            return Optional.empty();
        }

        var entryJson = entryNode.getValue();
        var uuid = UUID.fromString(entryJson.required("uuid").stringValue());
        var categoryUuid = Optional.ofNullable(entryJson.get("categoryUuid"))
                .map(jsonNode -> UUID.fromString(jsonNode.stringValue()))
                .orElse(DataStorage.DEFAULT_CATEGORY_UUID);
        var breakOutCategory = Optional.ofNullable(entryJson.get("breakOutCategoryUuid"))
                .filter(jsonNode -> !jsonNode.isNull())
                .map(jsonNode -> UUID.fromString(jsonNode.asString()))
                .orElse(null);
        var name = entryJson.required("name").stringValue().strip();

        // Fix for legacy issue where entries could have empty names
        if (name.isBlank()) {
            return Optional.empty();
        }

        var color = Optional.ofNullable(entryJson.get("color"))
                .map(node -> {
                    return mapper.treeToValue(node, DataStoreColor.class);
                })
                .orElse(null);
        var template = Optional.ofNullable(entryJson.get("template"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(false);
        var pinToTop = Optional.ofNullable(entryJson.get("pinToTop"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(false);
        var tags = Optional.ofNullable(entryJson.get("tags"))
                .map(jsonNode -> {
                    List<String> l = new ArrayList<>();
                    for (JsonNode node : jsonNode) {
                        var tag = node.asString();
                        if (!tag.isBlank()) {
                            l.add(tag);
                        }
                    }
                    return l;
                })
                .orElse(new ArrayList<>());

        var iconNode = entryJson.get("icon");
        String icon = iconNode != null && !iconNode.isNull() ? iconNode.asString() : null;
        double orderIndex = Optional.ofNullable(entryJson.get("orderIndex"))
                .map(jsonNode -> jsonNode.doubleValue())
                .orElse(0.0);
        if (orderIndex < 0.0) {
            orderIndex = 0.0;
        }

        DataStoreEntryNode<JsonNode> stateNode = DataStoreEntryNode.parse(stateFile, JsonNode.class);
        if (stateNode != null && !stateNode.isAccessible()) {
            stateNode = null;
        }

        var stateJson = stateNode != null ? stateNode.getValue() : JsonNodeFactory.instance.nullNode();
        var persistentState = stateJson.get("persistentState");
        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed"))
                .map(jsonNode -> jsonNode.stringValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var lastModified = Optional.ofNullable(stateJson.get("lastModified"))
                .map(jsonNode -> jsonNode.stringValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);
        var expanded = Optional.ofNullable(stateJson.get("expanded"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(true);

        var created = Optional.ofNullable(entryJson.get("created"))
                .map(jsonNode -> jsonNode.stringValue())
                .map(Instant::parse)
                .orElse(lastModified);

        if (color == null) {
            color = Optional.ofNullable(stateJson.get("color"))
                    .map(node -> {
                        return mapper.treeToValue(node, DataStoreColor.class);
                    })
                    .orElse(null);
        }

        DataStoreEntryNode<String> notesNode = null;
        if (Files.exists(normalNotesFile)) {
            notesNode = DataStoreEntryNode.ofWritten(Files.readString(normalNotesFile));
        } else if (Files.exists(encryptedNotesFile)) {
            notesNode = DataStoreEntryNode.parse(encryptedNotesFile, String.class);
        }
        if (notesNode != null
                && notesNode.isAccessible()
                && notesNode.getValue().isBlank()) {
            notesNode = null;
        }
        if (notesNode != null && !notesNode.isAccessible()) {
            notesNode = null;
        }

        DataStoreEntryNode<DataStore> storeNode = DataStoreEntryNode.parse(storeFile, DataStore.class);
        return Optional.of(new DataStoreEntry(
                dir,
                uuid,
                categoryUuid,
                name,
                created,
                lastUsed,
                lastModified,
                false,
                storeNode == null ? Validity.LOAD_FAILED : Validity.INCOMPLETE,
                persistentState,
                expanded,
                color,
                icon,
                orderIndex,
                template,
                pinToTop,
                breakOutCategory,
                tags,
                storeNode,
                stateNode,
                entryNode,
                notesNode));
    }

    public String getEffectiveIconFile() {
        if (getValidity() == Validity.LOAD_FAILED) {
            return "error.png";
        }

        if (icon == null) {
            return getProvider().getDisplayIconFileName(getStore());
        }

        var found = SystemIconManager.getIcon(icon);
        if (found.isPresent()) {
            return SystemIconManager.getAndLoadIconFile(found.get(), true);
        } else {
            return "error.png";
        }
    }

    public DataStore getStore() {
        return storeNode != null && storeNode.isAccessible() ? storeNode.getValue() : null;
    }

    public String getNotes() {
        return notesNode != null && notesNode.isAccessible() ? notesNode.getValue() : null;
    }

    public void setColor(DataStoreColor newColor) {
        var changed = !Objects.equals(color, newColor);
        this.color = newColor;
        if (changed) {
            notifyUpdate(false, true);
        }
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

    public DataStoreAccessScope getAccessScope() {
        if (storeNode != null && !storeNode.isAccessible()) {
            var enc = storeNode.getEncryptedValue();
            var secret = enc.getSecret();
            return secret != null ? secret.getScope() : DataStoreAccessScope.encryption();
        }

        try {
            if (getStore() instanceof AccessScopeStore s) {
                return s.getAccessScope();
            }
        } catch (Exception ignored) {
        }

        var defParent = DataStorage.get().getDefaultDisplayParent(this);
        if (defParent.isPresent()) {
            return defParent.get().getAccessScope();
        }

        return DataStoreAccessScope.encryption();
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
        if (!(getStore() instanceof StatefulDataStore<?> sds)) {
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

    public void setStorePersistentState(DataStoreState value) {
        var changed = !Objects.equals(storePersistentState, value);
        this.storePersistentState = value;
        this.storePersistentStateNode = JacksonMapper.getDefault().valueToTree(value);
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

    public void addTag(String tag) {
        if (tags == null || tag == null || tag.isBlank()) {
            return;
        }

        tag = tag.strip();

        if (tags.contains(tag)) {
            return;
        }

        tags.add(tag);
        notifyUpdate(false, true);
    }

    public void removeTag(String tag) {
        if (tags == null || tag == null || tag.isBlank()) {
            return;
        }

        tag = tag.strip();

        if (tags.remove(tag)) {
            notifyUpdate(false, true);
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
    public boolean isInStorage() {
        return DataStorage.get().getStoreEntries().contains(this);
    }

    @Override
    public Path[] getShareableFiles() {
        var notes = directory.resolve("notes.md");
        var list = List.of(directory.resolve("store.json"), directory.resolve("entry.json"));
        return Stream.concat(list.stream(), Files.exists(notes) ? Stream.of(notes) : Stream.of())
                .toArray(Path[]::new);
    }

    public boolean isAccessible() {
        return (storeNode == null || storeNode.isAccessible())
                && (entryNode == null || entryNode.isAccessible())
                && (stateNode == null || stateNode.isAccessible())
                && (notesNode == null || notesNode.isAccessible());
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

        ObjectNode entryNode = JsonNodeFactory.instance.objectNode();
        entryNode.put("uuid", uuid.toString());
        entryNode.put("name", name);
        entryNode.put("categoryUuid", categoryUuid.toString());
        entryNode.put("breakOutCategoryUuid", breakOutCategory != null ? breakOutCategory.toString() : null);
        entryNode.set("color", mapper.valueToTree(color));
        entryNode.set("icon", mapper.valueToTree(icon));
        entryNode.put("template", template);
        entryNode.put("pinToTop", pinToTop);
        entryNode.put("orderIndex", orderIndex);
        entryNode.put("created", created.toString());

        var tagsArray = entryNode.putArray("tags");
        for (String tag : tags) {
            tagsArray.add(tag);
        }

        ObjectNode stateNode = JsonNodeFactory.instance.objectNode();
        stateNode.put("lastUsed", lastUsed.toString());
        stateNode.put("lastModified", lastModified.toString());
        stateNode.set("persistentState", storePersistentStateNode);
        stateNode.put("expanded", expanded);

        FileUtils.forceMkdir(directory.toFile());

        this.stateNode = this.stateNode != null
                ? this.stateNode.prepareForWrite(this, false, stateNode)
                : DataStoreEntryNode.of(stateNode);
        if (this.stateNode.requiresWrite()) {
            Files.writeString(directory.resolve("state.json"), this.stateNode.getWriteString());
        }

        this.entryNode = this.entryNode != null
                ? this.entryNode.prepareForWrite(this, false, entryNode)
                : DataStoreEntryNode.of(entryNode);
        if (this.entryNode.requiresWrite()) {
            Files.writeString(directory.resolve("entry.json"), this.entryNode.getWriteString());
        }

        this.notesNode = this.notesNode != null ? this.notesNode.prepareForWrite(this, false, getNotes()) : null;
        if (this.notesNode != null && this.notesNode.requiresWrite()) {
            var normalNotesFile = directory.resolve("notes.md");
            var encryptedNotesFile = directory.resolve("notes.json");
            Files.deleteIfExists(normalNotesFile);
            Files.deleteIfExists(encryptedNotesFile);
            var file = this.notesNode.isEncrypted() ? encryptedNotesFile : normalNotesFile;
            Files.writeString(file, this.notesNode.getWriteString());
        }

        this.storeNode = this.storeNode.prepareForWrite(this, true, getStore());
        if (this.storeNode.requiresWrite()) {
            Files.writeString(directory.resolve("store.json"), this.storeNode.getWriteString());
        }
    }

    public void setNotes(String newNotes) {
        var changed = !Objects.equals(getNotes(), newNotes);
        if (changed) {
            this.notesNode = DataStoreEntryNode.of(newNotes);
            notifyUpdate(false, true);
        }
    }

    public void setTemplate(boolean newValue) {
        var changed = template != newValue;
        this.template = newValue;
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
        validity = e.validity;
        provider = e.provider;
        childrenCache = null;
        storeCache.clear();
        storeCache.putAll(e.storeCache);
        validity = getStore() == null
                ? Validity.LOAD_FAILED
                : getStore().isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE;
        storePersistentState = e.storePersistentState;
        storePersistentStateNode = e.storePersistentStateNode;
        icon = e.icon;
        categoryUuid = e.categoryUuid;
        notifyUpdate(false, true);
    }

    void setStoreInternal(DataStore store, boolean updateTime) {
        var changed = !Objects.equals(getStore(), store);
        if (!changed) {
            return;
        }

        if (!storeNode.isAccessible()) {
            return;
        }

        this.storeNode = DataStoreEntryNode.of(store);
        this.provider = DataStoreProvider.byStore(store);
        this.validity =
                store != null ? (store.isComplete() ? Validity.COMPLETE : Validity.INCOMPLETE) : Validity.LOAD_FAILED;
        if (updateTime) {
            lastModified = Instant.now();
        }
        childrenCache = null;
        dirty = true;
        notifyUpdate(false, updateTime);
    }

    public void validate() {
        try {
            validateOrThrow();
        } catch (Throwable ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
        }
    }

    public void validateOrThrow() throws Throwable {
        if (getStore() == null) {
            return;
        }

        try {
            incrementBusyCounter();
            getStore().checkComplete();
            if ((getStore() instanceof ValidatableStore l)) {
                l.validate();
            }
        } finally {
            decrementBusyCounter();
        }
    }

    public boolean refreshStoreEncryption() {
        if (storeNode == null) {
            return false;
        }

        var newNode = storeNode.prepareForWrite(this, true, getStore() instanceof EncryptionStore s ? s.withUpdatedPrincipals() : getStore());
        if (!newNode.equals(storeNode)) {
            storeNode = newNode;
            dirty = newNode.requiresWrite();
            notifyUpdate(false, false);
            var valid = storeNode.getValue() != null;
            validity = valid ? validity : Validity.LOAD_FAILED;
            provider = valid ? provider : null;
            return true;
        } else {
            return false;
        }
    }

    public void refreshStore() {
        if (validity == Validity.LOAD_FAILED) {
            return;
        }

        DataStore newStore;
        try {
            newStore = storeNode.reparseValue(DataStore.class);

            // Update any outdated principals for the store
            if (newStore instanceof EncryptionStore s) {
                newStore = s.withUpdatedPrincipals();
            }

            if (newStore != null) {
                // Check whether we have a provider as well
                DataStoreProvider.byStore(newStore);
            }
        } catch (Throwable e) {
            ErrorEventFactory.fromThrowable(e).handle();
            newStore = null;
        }

        if (newStore == null) {
            var changed = getStore() != null;
            storeNode = null;
            provider = null;
            validity = Validity.LOAD_FAILED;
            if (changed) {
                notifyUpdate(false, false);
            }
            return;
        }

        try {
            var newComplete = newStore.isComplete();
            if (!newComplete) {
                var changed = !Objects.equals(getStore(), newStore) || validity != Validity.INCOMPLETE;
                validity = Validity.INCOMPLETE;
                storeNode = storeNode.with(newStore);
                dirty = storeNode.requiresWrite();
                provider = DataStoreProvider.byStore(getStore());
                if (changed) {
                    notifyUpdate(false, false);
                }
                return;
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).omit().handle();
            return;
        }

        DataStoreAccessScope newAccessScope = null;
        try {
            if (newStore instanceof AccessScopeStore u) {
                newAccessScope = u.getAccessScope();
            }
        } catch (Exception ignored) {
        }
        var storeChanged = !Objects.equals(getStore(), newStore);
        if (storeChanged) {
            storeNode = storeNode.with(newStore);
            provider = DataStoreProvider.byStore(getStore());
            dirty = storeNode.requiresWrite();
        }
        var changed =
                storeChanged || validity != Validity.COMPLETE || !Objects.equals(getAccessScope(), newAccessScope);
        validity = Validity.COMPLETE;
        if (changed) {
            notifyUpdate(false, false);
        }
    }

    public void finalizeEntry() {
        if (getStore() instanceof ExpandedLifecycleStore lifecycleStore) {
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
        if (getStore() instanceof ExpandedLifecycleStore) {
            ThreadHelper.runAsync(() -> {
                finalizeEntry();
            });
            return true;
        } else {
            return false;
        }
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
