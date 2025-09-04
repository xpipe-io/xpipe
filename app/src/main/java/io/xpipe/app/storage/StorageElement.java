package io.xpipe.app.storage;

import io.xpipe.app.issue.ErrorEventFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.NonFinal;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
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

    public StorageElement(
            Path directory,
            UUID uuid,
            String name,
            Instant lastUsed,
            Instant lastModified,
            boolean expanded,
            boolean dirty) {
        this.directory = directory;
        this.uuid = uuid;
        this.name = name;
        this.lastUsed = lastUsed;
        this.lastModified = lastModified;
        this.expanded = expanded;
        this.dirty = dirty;
    }

    public Instant getStorageCreationDate() {
        if (!Files.exists(directory)) {
            return Instant.now();
        }

        try {
            return Files.getLastModifiedTime(directory).toInstant();
        } catch (IOException e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return Instant.now();
        }
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
        synchronized (listeners) {
            listeners.forEach(l -> l.onUpdate());
        }

        // Save changes instantly
        if (modified) {
            DataStorage.get().saveAsync();
        }
    }

    public void addListener(Listener l) {
        synchronized (listeners) {
            this.listeners.add(l);
        }
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

        notifyUpdate(false, true);
    }

    public void setLastUsed(Instant lastUsed) {
        if (lastUsed.equals(this.lastUsed)) {
            return;
        }

        notifyUpdate(true, false);
    }

    public interface Listener {
        void onUpdate();
    }
}
