package io.xpipe.app.storage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class StorageElement {

    @Getter
    protected final UUID uuid;

    protected final List<Listener> listeners = new ArrayList<>();

    @Getter
    protected boolean dirty;

    @Setter
    @Getter
    protected Path directory;

    @Getter
    protected String name;

    @Getter
    protected Instant lastUsed;

    @Getter
    protected Instant lastModified;

    @NonFinal
    @Getter
    protected boolean expanded;

    protected @NonFinal @Getter DataColor color;

    public StorageElement(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            DataColor color,
            boolean expanded,
            boolean dirty) {
        this.directory = directory;
        this.uuid = uuid;
        this.name = name;
        this.lastUsed = lastUsed;
        this.lastModified = lastModified;
        this.color = color;
        this.expanded = expanded;
        this.dirty = dirty;
    }

    public void setExpanded(boolean expanded) {
        var changed = expanded != this.expanded;
        this.expanded = expanded;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public abstract Path[] getShareableFiles();

    public void notifyUpdate(boolean used, boolean modified) {
        if (used) {
            lastUsed = Instant.now();
            dirty = true;
        }
        if (modified) {
            lastModified = Instant.now();
            dirty = true;
        }
        listeners.forEach(l -> l.onUpdate());
    }

    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public final void deleteFromDisk() throws IOException {
        FileUtils.deleteDirectory(directory.toFile());
    }

    public void setColor(DataColor newColor) {
        var changed = !Objects.equals(color, newColor);
        this.color = newColor;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public abstract void writeDataToDisk() throws Exception;

    public synchronized Instant getLastAccess() {
        if (getLastUsed() == null) {
            return getLastModified();
        }

        return getLastUsed().isAfter(getLastModified()) ? getLastUsed() : getLastModified();
    }

    public void setName(String name) {
        if (name.equals(this.name)) {
            return;
        }

        this.name = name;
        notifyUpdate(false, true);
    }

    public void setLastModified(Instant lastModified) {
        if (lastModified.equals(this.lastModified)) {
            return;
        }

        this.lastModified = lastModified;
        notifyUpdate(false, false);
    }


    public void setLastUsed(Instant lastUsed) {
        if (lastUsed.equals(this.lastUsed)) {
            return;
        }

        this.lastUsed = lastUsed;
        notifyUpdate(false, false);
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
