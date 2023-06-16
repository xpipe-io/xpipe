package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.app.ext.DataSourceProvider;
import io.xpipe.app.ext.DataSourceProviders;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.JacksonMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = true)
public class DataSourceEntry extends StorageElement {

    @NonFinal
    State state;

    @NonFinal
    String information;

    @NonFinal
    JsonNode sourceNode;

    @NonFinal
    DataSource<?> dataSource;

    private DataSourceEntry(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            boolean dirty,
            JsonNode sourceNode,
            String info,
            State state) {
        super(directory, uuid, name, lastUsed, lastModified, dirty);
        this.sourceNode = sourceNode;
        this.dataSource = DataStorageParser.sourceFromNode(sourceNode);
        this.information = info;
        this.state = state;
    }

    public static DataSourceEntry createExisting(
            @NonNull Path directory,
            @NonNull UUID uuid,
            @NonNull String name,
            @NonNull Instant lastUsed,
            @NonNull Instant lastModified,
            JsonNode sourceNode,
            String info,
            @NonNull State state)
            throws Exception {
        var entry = new DataSourceEntry(directory, uuid, name, lastUsed, lastModified, false, sourceNode, info, state);
        entry.refresh(false);
        return entry;
    }

    public static DataSourceEntry createNew(
            @NonNull UUID uuid, @NonNull String name, @NonNull DataSource<?> dataSource) {
        var entry = new DataSourceEntry(
                null,
                uuid,
                name,
                Instant.now(),
                Instant.now(),
                true,
                DataStorageWriter.sourceToNode(dataSource),
                null,
                State.INCOMPLETE);
        entry.simpleRefresh();
        return entry;
    }

    public static DataSourceEntry fromDirectory(Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.newMapper();

        var entryFile = dir.resolve("entry.json");
        var sourceFile = dir.resolve("source.json");
        if (!Files.exists(entryFile) || !Files.exists(sourceFile)) {
            return null;
        }

        var json = mapper.readTree(entryFile.toFile());
        var uuid = UUID.fromString(json.required("uuid").textValue());
        var name = json.required("name").textValue();
        var info = json.required("info").textValue();
        var lastUsed = Instant.parse(json.required("lastUsed").textValue());
        var lastModified = Instant.parse(json.required("lastModified").textValue());
        var state = mapper.treeToValue(json.get("state"), State.class);
        if (state == null) {
            state = State.INCOMPLETE;
        }

        // Source loading is prone to errors.
        // Therefore, handle invalid source in a special way
        JsonNode sourceNode = null;
        try {
            sourceNode = mapper.readTree(sourceFile.toFile());
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }

        return createExisting(dir, uuid, name, lastUsed, lastModified, sourceNode, info, state);
    }

    public void writeDataToDisk() throws Exception {
        if (!dirty) {
            return;
        }

        ObjectMapper mapper = JacksonMapper.newMapper();
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("info", information);
        obj.put("name", name);
        obj.put("lastUsed", lastUsed.toString());
        obj.put("lastModified", lastModified.toString());

        var entryString = mapper.writeValueAsString(obj);
        var sourceString = mapper.writeValueAsString(dataSource);

        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("entry.json"), entryString);
        Files.writeString(directory.resolve("source.json"), sourceString);

        this.dirty = false;
    }

    public void refresh(boolean deep) throws Exception {
        var oldSource = dataSource;
        DataSource<?> newSource = DataStorageParser.sourceFromNode(sourceNode);

        //        TrackEvent.storage()
        //                .trace()
        //                .message("Refreshing data source entry")
        //                .tag("old", oldSource)
        //                .tag("new", newSource)
        //                .handle();

        if (newSource == null) {
            dataSource = null;
            state = State.LOAD_FAILED;
            information = null;
            dirty = oldSource != null;
            propagateUpdate();
            return;
        }

        dataSource = newSource;
        dirty = true;

        if (state == State.LOAD_FAILED || state == State.INCOMPLETE) {
            state = newSource.isComplete() ? State.COMPLETE_BUT_INVALID : State.INCOMPLETE;
            propagateUpdate();
        }

        if (!deep || !dataSource.isComplete()) {
            return;
        }

        try {
            state = State.VALIDATING;
            propagateUpdate();

            dataSource.validate();

            state = State.COMPLETE_AND_VALID;
            information = getProvider().queryInformationString(getStore(), 50);
            propagateUpdate();
        } catch (Exception e) {
            state = dataSource.isComplete() ? State.COMPLETE_BUT_INVALID : State.INCOMPLETE;
            information = null;
            propagateUpdate();
            throw e;
        }
    }

    @Override
    protected boolean shouldSave() {
        return true;
    }

    public DataSourceProvider<?> getProvider() {
        if (state == State.LOAD_FAILED) {
            return null;
        }

        return DataSourceProviders.byDataSourceClass(dataSource.getClass());
    }

    public DataSourceType getDataSourceType() {
        return getProvider() != null ? getProvider().getPrimaryType() : null;
    }

    public DataStore getStore() {
        return dataSource.getStore();
    }

    public DataSource<?> getSource() {
        return dataSource;
    }

    public void setSource(DataSource<?> dataSource) {
        this.dirty = true;
        this.dataSource = dataSource;
        this.sourceNode = DataStorageWriter.sourceToNode(dataSource);
        propagateUpdate();
    }

    @Getter
    public enum State {
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
