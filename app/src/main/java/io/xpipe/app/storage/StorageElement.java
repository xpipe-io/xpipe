package io.xpipe.app.storage;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class StorageElement {

    @Getter
    protected final UUID uuid;

    protected final List<Listener> listeners = new ArrayList<>();

    @Getter
    protected boolean dirty;

    @Getter
    protected Path directory;

    @Getter
    protected String name;

    @Getter
    protected Instant lastUsed;

    @Getter
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

    public abstract Path[] getShareableFiles();

    public void updateLastUsed() {
        this.lastUsed = Instant.now();
        this.dirty = true;
        notifyUpdate();
    }

    protected void notifyUpdate() {
        lastModified = Instant.now();
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

    public void setDirectory(Path directory) {
        this.directory = directory;
    }

    public void setName(String name) {
        if (name.equals(this.name)) {
            return;
        }

        this.name = name;
        this.dirty = true;
        this.lastModified = Instant.now();
        notifyUpdate();
    }

    public interface Listener {
        void onUpdate();
    }

    @Builder
    @Jacksonized
    @Value
    public static class Configuration {
        boolean deletable;

        public static Configuration defaultConfiguration() {
            return new Configuration(true);
        }
    }
}
