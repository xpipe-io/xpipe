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
import java.util.Objects;
import java.util.UUID;

public abstract class DataStorageElement {

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
    protected final Instant created;

    @Getter
    protected Instant lastUsed;

    @Getter
    protected Instant lastModified;

    @NonFinal
    @Getter
    protected boolean expanded;

    @NonFinal
    @Getter
    protected String icon;

    @Getter
    @NonFinal
    protected double orderIndex;

    public DataStorageElement(
            Path directory,
            UUID uuid,
            String name,
            Instant created,
            Instant lastUsed,
            Instant lastModified,
            boolean expanded,
            boolean dirty,
            String icon,
            double orderIndex) {
        this.directory = directory;
        this.uuid = uuid;
        this.name = name;
        this.created = created;
        this.lastUsed = lastUsed;
        this.lastModified = lastModified;
        this.expanded = expanded;
        this.dirty = dirty;
        this.icon = icon;
        this.orderIndex = orderIndex;
    }

    public void setOrderIndex(double orderIndex) {
        var changed = this.orderIndex != orderIndex;
        this.orderIndex = orderIndex;
        if (changed) {
            notifyUpdate(false, true);
        }
    }

    public void setIcon(String icon, boolean force) {
        if (this.icon != null && !force) {
            return;
        }

        var changed = !Objects.equals(this.icon, icon);
        this.icon = icon;
        if (changed) {
            notifyUpdate(false, true);
        }
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
        if (!changed) {
            return;
        }

        this.expanded = expanded;

        // Update state but don't register updated time for expanded change
        this.dirty = true;
        synchronized (listeners) {
            listeners.forEach(l -> l.onUpdate());
        }

        // Save changes instantly
        if (isInStorage()) {
            DataStorage.get().saveAsync();
        }
    }

    public abstract boolean isInStorage();

    public abstract List<Path> getSyncableFiles();

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
        if (modified && isInStorage()) {
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

        if (name.isBlank()) {
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
