package io.xpipe.app.storage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.xpipe.core.util.JacksonMapper;
import org.apache.commons.io.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class DataSourceCollection extends StorageElement {

    private final Map<UUID, DataSourceEntry> entries;
    private final List<CollectionListener> listeners;

    private DataSourceCollection(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            Map<UUID, DataSourceEntry> entries,
            boolean dirty) {
        super(directory, uuid, name, lastUsed, lastModified, dirty);
        this.entries = new LinkedHashMap<>(entries);
        this.listeners = new ArrayList<>();
        this.listeners.add(new CollectionListener() {
            @Override
            public void onUpdate() {
                DataSourceCollection.this.dirty = true;
                DataSourceCollection.this.lastModified = Instant.now();
            }

            @Override
            public void onEntryAdd(DataSourceEntry entry) {
                DataSourceCollection.this.dirty = true;
                DataSourceCollection.this.lastModified = Instant.now();
            }

            @Override
            public void onEntryRemove(DataSourceEntry entry) {
                DataSourceCollection.this.dirty = true;
                DataSourceCollection.this.lastModified = Instant.now();
            }
        });
    }

    public static DataSourceCollection createNew(String name) {
        var c = new DataSourceCollection(null, UUID.randomUUID(), name, null, Instant.now(), Map.of(), true);
        return c;
    }

    public static DataSourceCollection fromDirectory(DataStorage storage, Path dir) throws Exception {
        ObjectMapper mapper = JacksonMapper.newMapper();

        var json = mapper.readTree(dir.resolve("collection.json").toFile());
        var uuid = UUID.fromString(json.required("uuid").textValue());
        var name = json.required("name").textValue();
        Objects.requireNonNull(name);
        var lastModified = Instant.parse(json.required("lastModified").textValue());

        JavaType listType = mapper.getTypeFactory().constructCollectionType(ArrayList.class, UUID.class);
        var entries = new LinkedHashMap<UUID, DataSourceEntry>();
        for (var u : mapper.<List<UUID>>readValue(dir.resolve("entries.json").toFile(), listType)) {
            var v = storage.getSourceEntryByUuid(u).orElse(null);
            entries.put(u, v);
        }

        Instant lastUsed = entries.values().stream()
                .filter(Objects::nonNull)
                .map(DataSourceEntry::getLastUsed)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new DataSourceCollection(dir, uuid, name, lastUsed, lastModified, entries, false);
    }

    public void writeDataToDisk() throws Exception {
        if (!dirty) {
            return;
        }

        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("uuid", uuid.toString());
        obj.put("name", name);
        obj.put("lastModified", lastModified.toString());

        ObjectMapper mapper = JacksonMapper.newMapper();
        var collectionString = mapper.writeValueAsString(obj);
        var entriesString = mapper.writeValueAsString(entries.keySet().stream().toList());

        FileUtils.forceMkdir(directory.toFile());
        Files.writeString(directory.resolve("collection.json"), collectionString);
        Files.writeString(directory.resolve("entries.json"), entriesString);

        this.dirty = false;
    }

    public void addListener(CollectionListener l) {
        this.listeners.add(l);
    }

    public void addEntry(DataSourceEntry e) {
        this.entries.put(e.getUuid(), e);
        this.listeners.forEach(l -> l.onEntryAdd(e));
    }

    public void removeEntry(DataSourceEntry e) {
        this.entries.remove(e.getUuid());
        this.listeners.forEach(l -> l.onEntryRemove(e));
    }

    @Override
    public void refresh(boolean deep) throws Exception {}

    public void setName(String name) {
        if (name.equals(this.name)) {
            return;
        }

        var oldName = this.name;
        this.name = name;
        this.listeners.forEach(l -> l.onUpdate());
    }

    @Override
    protected boolean shouldSave() {
        return true;
    }

    public void clear() {
        entries.clear();
    }

    public List<DataSourceEntry> getEntries() {
        return entries.values().stream().filter(Objects::nonNull).toList();
    }
}
