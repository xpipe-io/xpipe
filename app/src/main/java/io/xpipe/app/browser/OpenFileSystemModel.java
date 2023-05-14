/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.XPipeDaemon;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.ConnectionFileSystem;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.function.FailableConsumer;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
public final class OpenFileSystemModel {

    private Property<FileSystemStore> store = new SimpleObjectProperty<>();
    private FileSystem fileSystem;
    private final Property<String> filter = new SimpleStringProperty();
    private final FileListModel fileList;
    private final ReadOnlyObjectWrapper<String> currentPath = new ReadOnlyObjectWrapper<>();
    private final FileBrowserHistory history = new FileBrowserHistory();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final FileBrowserModel browserModel;
    private final BooleanProperty noDirectory = new SimpleBooleanProperty();
    private final Property<OpenFileSystemSavedState> savedState = new SimpleObjectProperty<>();
    private final OpenFileSystemCache cache = new OpenFileSystemCache(this);

    public OpenFileSystemModel(FileBrowserModel browserModel) {
        this.browserModel = browserModel;
        fileList = new FileListModel(this);
        addListeners();
    }

    public void withShell(FailableConsumer<ShellControl, Exception> c, boolean refresh) {
        ThreadHelper.runFailableAsync(() -> {
            if (fileSystem == null) {
                return;
            }

            BusyProperty.execute(busy, () -> {
                if (store.getValue() instanceof ShellStore s) {
                    c.accept(fileSystem.getShell().orElseThrow());
                    if (refresh) {
                        refreshSync();
                    }
                }
            });
        });
    }

    private void addListeners() {
        savedState.addListener((observable, oldValue, newValue) -> {
            if (store.getValue() == null) {
                return;
            }

            var storageEntry = DataStorage.get().getStoreEntryIfPresent(store.getValue());
            storageEntry.ifPresent(entry -> AppCache.update("browser-state-" + entry.getUuid(), newValue));
        });

        currentPath.addListener((observable, oldValue, newValue) -> {
            savedState.setValue(savedState.getValue().withLastDirectory(newValue));
        });
    }

    @SneakyThrows
    public void refresh() {
        BusyProperty.execute(busy, () -> {
            cdSyncWithoutCheck(currentPath.get());
        });
    }

    public void refreshSync() throws Exception {
        cdSyncWithoutCheck(currentPath.get());
    }

    public FileSystem.FileEntry getCurrentParentDirectory() {
        var current = getCurrentDirectory();
        if (current == null) {
            return null;
        }

        var parent = FileNames.getParent(currentPath.get());
        if (parent == null) {
            return null;
        }

        return new FileSystem.FileEntry(fileSystem, parent, null, true, false, false, 0, null);
    }

    public FileSystem.FileEntry getCurrentDirectory() {
        if (currentPath.get() == null) {
            return null;
        }

        return new FileSystem.FileEntry(fileSystem, currentPath.get(), null, true, false, false, 0, null);
    }

    public Optional<String> cd(String path) {
        if (Objects.equals(path, currentPath.get())) {
            return Optional.empty();
        }

        String newPath = null;
        try {
            newPath = FileSystemHelper.resolveDirectoryPath(this, path);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.of(currentPath.get());
        }

        if (!Objects.equals(path, newPath)) {
            return Optional.of(newPath);
        }

        ThreadHelper.runFailableAsync(() -> {
            try (var ignored = new BusyProperty(busy)) {
                cdSyncWithoutCheck(path);
            }
        });
        return Optional.empty();
    }

    private void cdSyncWithoutCheck(String path) throws Exception {
        if (fileSystem == null) {
            var fs = store.getValue().createFileSystem();
            fs.open();
            this.fileSystem = fs;
        }

        // Assume that the path is normalized to improve performance!
        // path = FileSystemHelper.normalizeDirectoryPath(this, path);

        filter.setValue(null);
        currentPath.set(path);
        savedState.setValue(savedState.getValue().withLastDirectory(path));
        history.updateCurrent(path);
        loadFilesSync(path);
    }

    private boolean loadFilesSync(String dir) {
        try {
            if (dir != null) {
                var stream = getFileSystem().listFiles(dir);
                noDirectory.set(false);
                fileList.setAll(stream);
            } else {
                var stream = getFileSystem().listRoots().stream()
                        .map(s -> new FileSystem.FileEntry(
                                getFileSystem(), s, Instant.now(), true, false, false, 0, null));
                noDirectory.set(true);
                fileList.setAll(stream);
            }
            return true;
        } catch (Exception e) {
            fileList.setAll(Stream.of());
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public void dropLocalFilesIntoAsync(FileSystem.FileEntry entry, List<Path> files) {
        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                FileSystemHelper.dropLocalFilesInto(entry, files);
                refreshSync();
            });
        });
    }

    public void dropFilesIntoAsync(
            FileSystem.FileEntry target, List<FileSystem.FileEntry> files, boolean explicitCopy) {
        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                var same = files.get(0).getFileSystem().equals(target.getFileSystem());
                if (same) {
                    if (!FileBrowserAlerts.showMoveAlert(files, target)) {
                        return;
                    }
                }

                FileSystemHelper.dropFilesInto(target, files, explicitCopy);
                refreshSync();
            });
        });
    }

    public void createDirectoryAsync(String name) {
        if (name.isBlank()) {
            return;
        }

        if (getCurrentDirectory() == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                var abs = FileNames.join(getCurrentDirectory().getPath(), name);
                if (fileSystem.directoryExists(abs)) {
                    throw new IllegalStateException(String.format("Directory %s already exists", abs));
                }

                fileSystem.mkdirs(abs);
                refreshSync();
            });
        });
    }

    public void createFileAsync(String name) {
        if (name.isBlank()) {
            return;
        }

        if (getCurrentDirectory() == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                var abs = FileNames.join(getCurrentDirectory().getPath(), name);
                fileSystem.touch(abs);
                refreshSync();
            });
        });
    }

    public void deleteSelectionAsync() {
        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (!FileBrowserAlerts.showDeleteAlert(fileList.getSelectedRaw())) {
                    return;
                }

                FileSystemHelper.delete(fileList.getSelectedRaw());
                refreshSync();
            });
        });
    }

    void closeSync() {
        if (fileSystem == null) {
            return;
        }

        try {
            fileSystem.close();
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
        }
        fileSystem = null;
        store = null;
    }

    public void switchSync(FileSystemStore fileSystem) throws Exception {
        BusyProperty.execute(busy, () -> {
            closeSync();
            this.store.setValue(fileSystem);
            var fs = fileSystem.createFileSystem();
            fs.open();
            this.fileSystem = fs;

            var storageEntry = DataStorage.get()
                    .getStoreEntryIfPresent(fileSystem)
                    .map(entry -> entry.getUuid())
                    .orElse(UUID.randomUUID());
            this.savedState.setValue(
                    AppCache.get("browser-state-" + storageEntry, OpenFileSystemSavedState.class, () -> {
                        try {
                            return OpenFileSystemSavedState.builder()
                                    .lastDirectory(FileSystemHelper.getStartDirectory(this))
                                    .build();
                        } catch (Exception e) {
                            ErrorEvent.fromThrowable(e).handle();
                            return null;
                        }
                    }));
            cdSyncWithoutCheck(this.savedState.getValue().getLastDirectory());
        });
    }

    public void openTerminalAsync(String directory) {
        ThreadHelper.runFailableAsync(() -> {
            if (fileSystem == null) {
                return;
            }

            BusyProperty.execute(busy, () -> {
                if (store.getValue() instanceof ShellStore s) {
                    var connection = ((ConnectionFileSystem) fileSystem).getShellControl();
                    var command = s.control()
                            .initWith(connection.getShellDialect().getCdCommand(directory))
                            .prepareTerminalOpen(directory + " - "
                                    + XPipeDaemon.getInstance()
                                            .getStoreName(store.getValue())
                                            .orElse("?"));
                    TerminalHelper.open(directory, command);
                }
            });
        });
    }

    public FileBrowserHistory getHistory() {
        return history;
    }

    public void back() {
        try (var ignored = new BusyProperty(busy)) {
            history.back().ifPresent(s -> cd(s));
        }
    }

    public void forth() {
        try (var ignored = new BusyProperty(busy)) {
            history.forth().ifPresent(s -> cd(s));
        }
    }
}
