package io.xpipe.app.storage;

import io.xpipe.core.util.XPipeSession;
import io.xpipe.extension.event.ErrorEvent;
import io.xpipe.extension.event.TrackEvent;
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
    private DataSourceCollection recovery;

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
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(name);
                    } catch (Exception ex) {
                        FileUtils.forceDelete(file.toFile());
                        return;
                    }

                    var entry = getStoreEntryByUuid(uuid);
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

        // Delete leftover directories in entries dir
        try (var s = Files.list(entriesDir)) {
            s.forEach(file -> {
                if (directoriesToKeep.contains(file)) {
                    return;
                }

                var name = file.getFileName().toString();
                try {
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(name);
                    } catch (Exception ex) {
                        FileUtils.forceDelete(file.toFile());
                        return;
                    }

                    var entry = getSourceEntryByUuid(uuid);
                    if (entry.isEmpty()) {
                        TrackEvent.withTrace("storage", "Deleting leftover entry directory")
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

        // Delete leftover directories in collections dir
        try (var s = Files.list(collectionsDir)) {
            s.forEach(file -> {
                if (directoriesToKeep.contains(file)) {
                    return;
                }

                var name = file.getFileName().toString();
                try {
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(name);
                    } catch (Exception ex) {
                        FileUtils.forceDelete(file.toFile());
                        return;
                    }

                    var col = getCollectionByUuid(uuid);
                    if (col.isEmpty()) {
                        TrackEvent.withTrace("storage", "Deleting leftover collection directory")
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

    public void load() {
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
                        directoriesToKeep.add(path);
                        ErrorEvent.fromThrowable(e).omitted(true).build().handle();
                    }
                });
                storeEntries.forEach(dataStoreEntry -> dataStoreEntry.simpleRefresh());
            }

            try (var dirs = Files.list(entriesDir)) {
                dirs.filter(Files::isDirectory).forEach(path -> {
                    try {
                        try (Stream<Path> list = Files.list(path)) {
                            if (list.findAny().isEmpty()) {
                                return;
                            }
                        }

                        var entry = DataSourceEntry.fromDirectory(path);
                        if (entry == null) {
                            return;
                        }

                        sourceEntries.add(entry);
                    } catch (Exception e) {
                        directoriesToKeep.add(path);
                        ErrorEvent.fromThrowable(e).omitted(true).build().handle();
                    }
                });
            }

            try (var dirs = Files.list(collectionsDir)) {
                dirs.filter(Files::isDirectory).forEach(path -> {
                    try {
                        try (Stream<Path> list = Files.list(path)) {
                            if (list.findAny().isEmpty()) {
                                return;
                            }
                        }

                        var col = DataSourceCollection.fromDirectory(this, path);

                        // Empty temp on new session
                        if (col.getName() == null && newSession) {
                            for (var e : col.getEntries()) {
                                var file = e.getDirectory();
                                TrackEvent.withTrace("storage", "Deleting temporary entry directory")
                                        .tag("uuid", e.getUuid())
                                        .handle();
                                FileUtils.forceDelete(file.toFile());
                            }
                            col.clear();
                        }

                        sourceCollections.add(col);
                    } catch (Exception e) {
                        directoriesToKeep.add(path);
                        ErrorEvent.fromThrowable(e).omitted(true).build().handle();
                    }
                });
            }

            // Add temporary collection it is not added yet
            getInternalCollection();

            for (var e : getSourceEntries()) {
                var inCol = getSourceCollections().stream()
                        .anyMatch(col -> col.getEntries().contains(e));
                if (!inCol) {
                    recovery = createOrGetCollection("Recovery");
                    recovery.addEntry(e);
                }
            }
        } catch (IOException ex) {
            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
        }

        deleteLeftovers();
    }

    public void save() {
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

        // Save entries
        sourceEntries.stream()
                .filter(dataStoreEntry -> dataStoreEntry.shouldSave())
                .forEach(e -> {
                    try {
                        e.writeDataToDisk();
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
                    }
                });

        // Save collections
        for (var c : sourceCollections) {
            if (c.equals(recovery)) {
                continue;
            }

            try {
                c.writeDataToDisk();
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
            }
        }

        deleteLeftovers();
    }

    @Override
    public Path getInternalStreamPath(@NonNull UUID uuid) {
        var newDir = getStreamsDir().resolve(uuid.toString());
        return newDir;
    }
}
