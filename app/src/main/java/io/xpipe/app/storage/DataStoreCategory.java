package io.xpipe.app.storage;

import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.app.util.JacksonMapper;

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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class DataStoreCategory extends DataStorageElement {

    @NonFinal
    UUID parentCategory;

    @NonFinal
    DataStoreCategoryConfig config;

    public DataStoreCategory(
            Path directory,
            UUID uuid,
            String name,
            Instant created,
            Instant lastUsed,
            Instant lastModified,
            boolean dirty,
            String icon,
            double orderIndex,
            UUID parentCategory,
            boolean expanded,
            DataStoreCategoryConfig config) {
        super(directory, uuid, name, created, lastUsed, lastModified, expanded, dirty, icon, orderIndex);
        this.parentCategory = parentCategory;
        this.config = config;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof DataStoreCategory e && e.getUuid().equals(getUuid()));
    }

    @Override
    public int hashCode() {
        return getUuid().hashCode();
    }

    @Override
    public String toString() {
        return getName();
    }

    public static DataStoreCategory createNew(UUID parentCategory, @NonNull String name) {
        return createNew(parentCategory, UUID.randomUUID(), name);
    }

    public static DataStoreCategory createNew(UUID parentCategory, @NonNull UUID uuid, @NonNull String name) {
        var now = Instant.now();
        return new DataStoreCategory(
                null,
                uuid,
                name,
                now,
                now,
                now,
                true,
                null,
                0.0,
                parentCategory,
                true,
                DataStoreCategoryConfig.empty());
    }

    public static Optional<DataStoreCategory> fromDirectory(Path dir) throws IOException {
        ObjectMapper mapper = JacksonMapper.getDefault();

        var categoryFile = dir.resolve("category.json");
        var stateFile = dir.resolve("state.json");
        if (!Files.exists(categoryFile)) {
            return Optional.empty();
        }

        var categoryString = Files.readString(categoryFile);
        var stateString = Files.exists(stateFile) ? Files.readString(stateFile) : null;

        var categoryJson = mapper.readTree(categoryString);
        var stateJson = stateString != null ? mapper.readTree(stateString) : JsonNodeFactory.instance.nullNode();

        var uuid = UUID.fromString(categoryJson.required("uuid").stringValue());
        var parentUuid = Optional.ofNullable(categoryJson.get("parentUuid"))
                .filter(jsonNode -> !jsonNode.isNull())
                .map(jsonNode -> UUID.fromString(jsonNode.stringValue()))
                .orElse(null);
        var name = categoryJson.required("name").stringValue();
        var orderIndex = Optional.ofNullable(categoryJson.get("orderIndex"))
                .map(jsonNode -> jsonNode.doubleValue())
                .orElse(0.0);
        var created = Optional.ofNullable(categoryJson.get("created"))
                .map(jsonNode -> jsonNode.stringValue())
                .map(Instant::parse)
                .orElse(Instant.EPOCH);

        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed"))
                .map(jsonNode -> jsonNode.stringValue())
                .map(Instant::parse)
                .orElse(Instant.now());
        var lastModified = Optional.ofNullable(stateJson.get("lastModified"))
                .map(jsonNode -> jsonNode.stringValue())
                .map(Instant::parse)
                .orElse(Instant.now());
        var expanded = Optional.ofNullable(stateJson.get("expanded"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(true);
        var config = Optional.ofNullable(categoryJson.get("config"))
                .map(jsonNode -> {
                    return JacksonMapper.getDefault().treeToValue(jsonNode, DataStoreCategoryConfig.class);
                })
                .orElse(DataStoreCategoryConfig.empty());

        var share = Optional.ofNullable(categoryJson.get("share"))
                .map(JsonNode::asBoolean)
                .orElse(null);
        if (share != null) {
            config = config.withSync(share);
        }
        var color = Optional.ofNullable(categoryJson.get("color"))
                .map(node -> {
                    return mapper.treeToValue(node, DataStoreColor.class);
                })
                .orElse(null);
        if (color != null) {
            config = config.withColor(color);
        }

        var iconNode = categoryJson.get("icon");
        String icon = iconNode != null && !iconNode.isNull() ? iconNode.asString() : null;

        return Optional.of(new DataStoreCategory(
                dir,
                uuid,
                name,
                created,
                lastUsed,
                lastModified,
                false,
                icon,
                orderIndex,
                parentUuid,
                expanded,
                config));
    }

    public boolean setConfig(DataStoreCategoryConfig config) {
        var changed = !this.config.equals(config);
        if (changed) {
            this.config = config;
            notifyUpdate(false, true);
            return true;
        }
        return false;
    }

    public boolean isChangedForReload(DataStoreCategory other) {
        return !Objects.equals(getName(), other.getName())
                || !Objects.equals(getOrderIndex(), other.getOrderIndex())
                || !Objects.equals(getEffectiveIconFile(), other.getEffectiveIconFile())
                || !Objects.equals(getConfig(), other.getConfig())
                || !Objects.equals(getParentCategory(), other.getParentCategory());
    }

    public void setParentCategory(UUID parentCategory) {
        var changed = !Objects.equals(this.parentCategory, parentCategory);
        this.parentCategory = parentCategory;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public String getDefaultIconFile() {
        if (uuid.equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID)) {
            return "connectionsCategory_icon.svg";
        }

        if (uuid.equals(DataStorage.DEFAULT_CATEGORY_UUID)) {
            return "connectionsCategory_icon.svg";
        }

        if (uuid.equals(DataStorage.ALL_IDENTITIES_CATEGORY_UUID)) {
            return "identityCategory_icon.svg";
        }

        if (uuid.equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID)) {
            return "localIdentity_icon.svg";
        }

        if (uuid.equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID)) {
            return "syncedIdentity_icon.svg";
        }

        if (uuid.equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID)) {
            return "scriptCategory_icon.svg";
        }

        if (uuid.equals(DataStorage.CUSTOM_SCRIPTS_CATEGORY_UUID)) {
            return "scriptCategory_icon.svg";
        }

        if (uuid.equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID)) {
            return "scriptCollectionSource_icon.svg";
        }

        if (uuid.equals(DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID)) {
            return "defaultShell_icon.svg";
        }

        return "connectionsCategory_icon.svg";
    }

    public String getEffectiveIconFile() {
        if (icon == null) {
            return getDefaultIconFile();
        }

        var found = SystemIconManager.getIcon(icon);
        if (found.isPresent()) {
            return SystemIconManager.getAndLoadIconFile(found.get(), true);
        } else {
            return "error.png";
        }
    }

    public boolean canShare() {
        if (parentCategory == null) {
            return false;
        }

        if (getUuid().equals(DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID)) {
            return false;
        }

        if (getUuid().equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID)) {
            return false;
        }

        return true;
    }

    @Override
    public boolean isInStorage() {
        return DataStorage.get().getStoreCategories().contains(this);
    }

    @Override
    public List<Path> getSyncableFiles() {
        return List.of(directory.resolve("category.json"));
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
        obj.put("parentUuid", parentCategory != null ? parentCategory.toString() : null);
        obj.set("config", JacksonMapper.getDefault().valueToTree(config));
        obj.set("icon", mapper.valueToTree(icon));
        obj.put("orderIndex", orderIndex);
        obj.put("created", created.toString());

        ObjectNode stateObj = JsonNodeFactory.instance.objectNode();
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.put("expanded", expanded);

        var entryString = mapper.writeValueAsString(obj);
        var stateString = mapper.writeValueAsString(stateObj);
        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("category.json"), entryString);
        Files.writeString(directory.resolve("state.json"), stateString);
    }

    public void applyChanges(DataStoreCategory newCategory) {
        name = newCategory.getName();
        parentCategory = newCategory.getParentCategory();
        orderIndex = newCategory.getOrderIndex();
        icon = newCategory.getIcon();
        config = newCategory.getConfig();
        notifyUpdate(false, true);
    }
}
