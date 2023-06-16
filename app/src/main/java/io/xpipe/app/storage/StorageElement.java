package io.xpipe.app.storage;

import io.xpipe.app.issue.ErrorEvent;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public abstract class StorageElement {

    protected final UUID uuid;
    protected final List<Listener> listeners = new ArrayList<>();
    protected final Map<String, Object> elementState = new LinkedHashMap<>();
    protected boolean dirty;
    protected Path directory;

    protected String name;

    protected Instant lastUsed;
    protected Instant lastModified;

    public StorageElement(
            Path directory, UUID uuid, String name, Instant lastUsed, Instant lastModified, boolean dirty) {
        this.directory = directory;
        this.uuid = uuid;
        this.name = name;
        this.lastUsed = lastUsed;
        this.lastModified = lastModified;
        this.dirty = dirty;
    }

    public Map<String, Object> getElementState() {
        return elementState;
    }

    public void simpleRefresh() {
        try {
            refresh(false);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
        }
    }

    public abstract void refresh(boolean deep) throws Exception;

    public void updateLastUsed() {
        this.lastUsed = Instant.now();
        this.dirty = true;
        this.listeners.forEach(l -> l.onUpdate());
    }

    protected abstract boolean shouldSave();

    protected void propagateUpdate() {
        listeners.forEach(l -> l.onUpdate());
    }

    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public final void deleteFromDisk() throws IOException {
        FileUtils.deleteDirectory(directory.toFile());
    }

    public abstract void writeDataToDisk() throws Exception;

    public synchronized Instant getLastAccess() {
        if (getLastUsed() == null) {
            return getLastModified();
        }

        return getLastUsed().isAfter(getLastModified()) ? getLastUsed() : getLastModified();
    }

    public Path getDirectory() {
        return directory;
    }

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name.equals(this.name)) {
            return;
        }

        this.name = name;
        this.dirty = true;
        this.lastModified = Instant.now();
        propagateUpdate();
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public interface Listener {
        void onUpdate();
    }

    @Builder
    @Jacksonized
    @Value
    public static class Configuration {
        boolean deletable;
        boolean renameable;
        boolean editable;
        boolean refreshable;

        public static Configuration defaultConfiguration() {
            return new Configuration(true, true, true, true);
        }
    }
}
