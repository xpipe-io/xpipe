package io.xpipe.app.storage;

import io.xpipe.app.icon.SystemIconManager;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class DataStoreCategory extends StorageElement {

    @NonFinal
    UUID parentCategory;

    @NonFinal
    DataStoreCategoryConfig config;

    public DataStoreCategory(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            boolean dirty,
            String icon,
            UUID parentCategory,
            boolean expanded,
            DataStoreCategoryConfig config) {
        super(directory, uuid, name, lastUsed, lastModified, expanded, dirty, icon);
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
        return new DataStoreCategory(
                null,
                UUID.randomUUID(),
                name,
                Instant.now(),
                Instant.now(),
                true,
                null,
                parentCategory,
                true,
                DataStoreCategoryConfig.empty());
    }

    public static DataStoreCategory createNew(UUID parentCategory, @NonNull UUID uuid, @NonNull String name) {
        return new DataStoreCategory(
                null,
                uuid,
                name,
                Instant.now(),
                Instant.now(),
                true,
                null,
                parentCategory,
                true,
                DataStoreCategoryConfig.empty());
    }

    public static Optional<DataStoreCategory> fromDirectory(Path dir) throws IOException {
        ObjectMapper mapper = JacksonMapper.getDefault();

        var entryFile = dir.resolve("category.json");
        var stateFile = dir.resolve("state.json");
        if (!Files.exists(entryFile)) {
            return Optional.empty();
        }

        var stateJson =
                Files.exists(stateFile) ? mapper.readTree(stateFile.toFile()) : JsonNodeFactory.instance.objectNode();
        var json = mapper.readTree(entryFile.toFile());

        var uuid = UUID.fromString(json.required("uuid").textValue());
        var parentUuid = Optional.ofNullable(json.get("parentUuid"))
                .filter(jsonNode -> !jsonNode.isNull())
                .map(jsonNode -> UUID.fromString(jsonNode.textValue()))
                .orElse(null);
        var name = json.required("name").textValue();

        var lastUsed = Optional.ofNullable(stateJson.get("lastUsed"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.now());
        var lastModified = Optional.ofNullable(stateJson.get("lastModified"))
                .map(jsonNode -> jsonNode.textValue())
                .map(Instant::parse)
                .orElse(Instant.now());
        var expanded = Optional.ofNullable(stateJson.get("expanded"))
                .map(jsonNode -> jsonNode.booleanValue())
                .orElse(true);
        var config = Optional.ofNullable(json.get("config"))
                .map(jsonNode -> {
                    try {
                        return JacksonMapper.getDefault().treeToValue(jsonNode, DataStoreCategoryConfig.class);
                    } catch (JsonProcessingException e) {
                        return DataStoreCategoryConfig.empty();
                    }
                })
                .orElse(DataStoreCategoryConfig.empty());

        var share =
                Optional.ofNullable(json.get("share")).map(JsonNode::asBoolean).orElse(null);
        if (share != null) {
            config = config.withSync(share);
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
        if (color != null) {
            config = config.withColor(color);
        }

        var iconNode = json.get("icon");
        String icon = iconNode != null && !iconNode.isNull() ? iconNode.asText() : null;

        return Optional.of(new DataStoreCategory(
                dir, uuid, name, lastUsed, lastModified, false, icon, parentUuid, expanded, config));
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
    public Path[] getShareableFiles() {
        return new Path[] {directory.resolve("category.json")};
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
        ObjectNode stateObj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("name", name);
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.put("expanded", expanded);
        obj.put("parentUuid", parentCategory != null ? parentCategory.toString() : null);
        obj.set("config", JacksonMapper.getDefault().valueToTree(config));
        obj.set("icon", mapper.valueToTree(icon));

        var entryString = mapper.writeValueAsString(obj);
        var stateString = mapper.writeValueAsString(stateObj);
        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("category.json"), entryString);
        Files.writeString(directory.resolve("state.json"), stateString);
    }
}
