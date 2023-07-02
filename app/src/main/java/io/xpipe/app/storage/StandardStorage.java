package io.xpipe.app.storage;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.XPipeSession;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class StandardStorage extends DataStorage {

    private final List<Path> directoriesToKeep = new ArrayList<>();

    private boolean isNewSession() {
        return XPipeSession.get().isNewSystemSession();
    }

    private void deleteLeftovers() {
        var entriesDir = getSourcesDir().resolve("entries");
        var collectionsDir = getSourcesDir().resolve("collections");
        var storesDir = getStoresDir();

        // Delete leftover directories in entries dir
        try (var s = Files.list(storesDir)) {
            s.forEach(file -> {
                if (directoriesToKeep.contains(file)) {
                    return;
                }

                var name = file.getFileName().toString();
                try {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(name);
                    } catch (Exception ex) {
                        FileUtils.forceDelete(file.toFile());
                        return;
                    }

                    var entry = getStoreEntry(uuid);
                    if (entry.isEmpty()) {
                        TrackEvent.withTrace("storage", "Deleting leftover store directory")
                                .tag("uuid", uuid)
                                .handle();
                        FileUtils.forceDelete(file.toFile());
                    }
                } catch (Exception ex) {
                    ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                }
            });
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }
    }

    public synchronized void load() {
        var newSession = isNewSession();
        var entriesDir = getSourcesDir().resolve("entries");
        var collectionsDir = getSourcesDir().resolve("collections");
        var storesDir = getStoresDir();
        var streamsDir = getStreamsDir();

        try {
            FileUtils.forceMkdir(entriesDir.toFile());
            FileUtils.forceMkdir(collectionsDir.toFile());
            FileUtils.forceMkdir(storesDir.toFile());
            FileUtils.forceMkdir(streamsDir.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).terminal(true).build().handle();
        }

        try {
            try (var dirs = Files.list(storesDir)) {
                dirs.filter(Files::isDirectory).forEach(path -> {
                    try {
                        try (Stream<Path> list = Files.list(path)) {
                            if (list.findAny().isEmpty()) {
                                return;
                            }
                        }

                        var entry = DataStoreEntry.fromDirectory(path);
                        if (entry == null) {
                            return;
                        }

                        storeEntries.add(entry);
                    } catch (Exception e) {
                        // We only keep invalid entries in developer mode as there's no point in keeping them in
                        // production.
                        if (AppPrefs.get().developerMode().getValue()) {
                            directoriesToKeep.add(path);
                        }
                        ErrorEvent.fromThrowable(e).omitted(true).build().handle();
                    }
                });

                // Refresh to update state
                storeEntries.forEach(dataStoreEntry -> dataStoreEntry.simpleRefresh());

                // Remove even incomplete stores when in production
                if (!AppPrefs.get().developerMode().getValue()) {
                    storeEntries.removeIf(entry -> {
                        return !entry.getState().isUsable();
                    });
                }
            }
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }

        deleteLeftovers();
    }

    public synchronized void save() {
        var entriesDir = getSourcesDir().resolve("entries");
        var collectionsDir = getSourcesDir().resolve("collections");

        try {
            FileUtils.forceMkdir(entriesDir.toFile());
            FileUtils.forceMkdir(collectionsDir.toFile());
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).terminal(true).build().handle();
        }

        // Save stores
        storeEntries.stream()
                .filter(dataStoreEntry -> dataStoreEntry.shouldSave())
                .forEach(e -> {
                    try {
                        e.writeDataToDisk();
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                    }
                });
        deleteLeftovers();
    }

    @Override
    public Path getInternalStreamPath(@NonNull UUID uuid) {
        var newDir = getStreamsDir().resolve(uuid.toString());
        return newDir;
    }
}
