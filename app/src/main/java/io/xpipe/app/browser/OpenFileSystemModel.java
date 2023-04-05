/* SPDX-License-Identifier: MIT */

package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.BusyProperty;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.store.ConnectionFileSystem;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
final class OpenFileSystemModel {

    private Property<FileSystemStore> store = new SimpleObjectProperty<>();
    private FileSystem fileSystem;
    private final Property<String> filter = new SimpleStringProperty();
    private final FileListModel fileList;
    private final ReadOnlyObjectWrapper<String> currentPath = new ReadOnlyObjectWrapper<>();
    private final FileBrowserNavigationHistory history = new FileBrowserNavigationHistory();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final FileBrowserModel browserModel;
    private final BooleanProperty noDirectory = new SimpleBooleanProperty();

    public OpenFileSystemModel(FileBrowserModel browserModel) {
        this.browserModel = browserModel;
        fileList = new FileListModel(this);
    }

    @SneakyThrows
    public void refresh() {
        BusyProperty.execute(busy, () -> {
            cdSync(currentPath.get());
        });
    }

    private void refreshInternal() throws Exception {
        cdSync(currentPath.get());
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

        return new FileSystem.FileEntry(fileSystem, parent, null, true, false, false, 0);
    }

    public FileSystem.FileEntry getCurrentDirectory() {
        if (currentPath.get() == null) {
            return null;
        }

        return new FileSystem.FileEntry(fileSystem, currentPath.get(), null, true, false, false, 0);
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
                cdSync(path);
            }
        });
        return Optional.empty();
    }

    private void cdSync(String path) throws Exception {
        if (fileSystem == null) {
            var fs = store.getValue().createFileSystem();
            fs.open();
            this.fileSystem = fs;
        }

        // Assumed that the path is normalized to improve performance!
        // path = FileSystemHelper.normalizeDirectoryPath(this, path);

        navigateToSync(path);
        filter.setValue(null);
        currentPath.set(path);
        history.cd(path);
    }

    private boolean navigateToSync(String dir) {
        try {
            List<FileSystem.FileEntry> newList;
            if (dir != null) {
                newList = getFileSystem().listFiles(dir).collect(Collectors.toCollection(ArrayList::new));
                noDirectory.set(false);
            } else {
                newList = getFileSystem().listRoots().stream()
                        .map(s -> new FileSystem.FileEntry(getFileSystem(), s, Instant.now(), true, false, false, 0))
                        .collect(Collectors.toCollection(ArrayList::new));
                noDirectory.set(true);
            }
            fileList.setAll(newList);
            return true;
        } catch (Exception e) {
            fileList.setAll(List.of());
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
                refreshInternal();
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
                refreshInternal();
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
                refreshInternal();
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
                refreshInternal();
            });
        });
    }

    public void deleteSelectionAsync() {
        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (!FileBrowserAlerts.showDeleteAlert(fileList.getSelected())) {
                    return;
                }

                FileSystemHelper.delete(fileList.getSelected());
                refreshInternal();
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

    private void switchSync(FileSystemStore fileSystem) throws Exception {
        closeSync();
        this.store.setValue(fileSystem);
        var fs = fileSystem.createFileSystem();
        fs.open();
        this.fileSystem = fs;

        var current = FileSystemHelper.getStartDirectory(this);
        cdSync(current);
    }

    public void switchAsync(FileSystemStore fileSystem) {
        ThreadHelper.runFailableAsync(() -> {
            BusyProperty.execute(busy, () -> {
                switchSync(fileSystem);
            });
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
                    var command = s.create()
                            .initWith(List.of(connection.getShellDialect().getCdCommand(directory)))
                            .prepareTerminalOpen();
                    TerminalHelper.open(directory, command);
                }
            });
        });
    }

    public FileBrowserNavigationHistory getHistory() {
        return history;
    }

    public void back() {
        history.back().ifPresent(s -> cd(s));
    }

    public void forth() {
        history.forth().ifPresent(s -> cd(s));
    }
}
