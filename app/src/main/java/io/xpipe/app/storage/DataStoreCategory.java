package io.xpipe.app.storage;

import io.xpipe.app.comp.store.StoreSortMode;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Value
public class DataStoreCategory extends StorageElement {

    @NonFinal
    UUID parentCategory;

    @NonFinal
    StoreSortMode sortMode;

    @NonFinal
    boolean sync;

    public DataStoreCategory(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            DataColor color,
            boolean dirty,
            UUID parentCategory,
            StoreSortMode sortMode,
            boolean sync,
            boolean expanded) {
        super(directory, uuid, name, lastUsed, lastModified, color, expanded, dirty);
        this.parentCategory = parentCategory;
        this.sortMode = sortMode;
        this.sync = sync;
    }

    public static DataStoreCategory createNew(UUID parentCategory, @NonNull String name) {
        return new DataStoreCategory(
                null,
                UUID.randomUUID(),
                name,
                Instant.now(),
                Instant.now(),
                null,
                true,
                parentCategory,
                StoreSortMode.getDefault(),
                false,
                true);
    }

    public static DataStoreCategory createNew(UUID parentCategory, @NonNull UUID uuid, @NonNull String name) {
        return new DataStoreCategory(
                null,
                uuid,
                name,
                Instant.now(),
                Instant.now(),
                null,
                true,
                parentCategory,
                StoreSortMode.getDefault(),
                false,
                true);
    }

    public static Optional<DataStoreCategory> fromDirectory(Path dir) throws Exception {
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
        var color = Optional.ofNullable(json.get("color"))
                .map(node -> {
                    try {
                        return mapper.treeToValue(node, DataColor.class);
                    } catch (JsonProcessingException e) {
                        return null;
                    }
                })
                .orElse(null);
        var name = json.required("name").textValue();

        var sortMode = Optional.ofNullable(stateJson.get("sortMode"))
                .map(JsonNode::asText)
                .flatMap(string -> StoreSortMode.fromId(string))
                .orElse(StoreSortMode.getDefault());
        var share =
                Optional.ofNullable(json.get("share")).map(JsonNode::asBoolean).orElse(false);
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

        return Optional.of(new DataStoreCategory(
                dir, uuid, name, lastUsed, lastModified, color, false, parentUuid, sortMode, share, expanded));
    }

    public void setSortMode(StoreSortMode sortMode) {
        var changed = this.sortMode != sortMode;
        if (changed) {
            this.sortMode = sortMode;
            notifyUpdate(false, true);
        }
    }

    public void setSync(boolean newShare) {
        var changed = sync != newShare;
        if (changed) {
            this.sync = newShare;
            notifyUpdate(false, true);
        }
    }

    public void setParentCategory(UUID parentCategory) {
        this.parentCategory = parentCategory;
        notifyUpdate(false, true);
    }

    public boolean canShare() {
        if (parentCategory == null) {
            return false;
        }

        if (getUuid().equals(DataStorage.PREDEFINED_SCRIPTS_CATEGORY_UUID)) {
            return false;
        }

        return true;
    }

    public boolean shouldShareChildren() {
        if (parentCategory == null) {
            return true;
        }

        if (!canShare()) {
            return false;
        }

        return isSync();
    }

    @Override
    public Path[] getShareableFiles() {
        return new Path[] {directory.resolve("category.json")};
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
        obj.put("share", sync);
        obj.set("color", mapper.valueToTree(color));
        stateObj.put("lastUsed", lastUsed.toString());
        stateObj.put("lastModified", lastModified.toString());
        stateObj.put("sortMode", sortMode.getId());
        stateObj.put("expanded", expanded);
        obj.put("parentUuid", parentCategory != null ? parentCategory.toString() : null);

        var entryString = mapper.writeValueAsString(obj);
        var stateString = mapper.writeValueAsString(stateObj);
        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("category.json"), entryString);
        Files.writeString(directory.resolve("state.json"), stateString);
        dirty = false;
    }
}
