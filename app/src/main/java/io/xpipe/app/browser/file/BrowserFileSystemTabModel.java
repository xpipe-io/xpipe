package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserAbstractSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.TerminalLauncher;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellOpenFunction;
import io.xpipe.core.store.*;
import io.xpipe.core.util.FailableConsumer;
import io.xpipe.core.util.FailableRunnable;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Getter
public final class BrowserFileSystemTabModel extends BrowserStoreSessionTab<FileSystemStore> {

    private final Property<String> filter = new SimpleStringProperty();
    private final BrowserFileListModel fileList;
    private final ReadOnlyObjectWrapper<String> currentPath = new ReadOnlyObjectWrapper<>();
    private final BrowserFileSystemHistory history = new BrowserFileSystemHistory();
    private final Property<ModalOverlayComp.OverlayContent> overlay = new SimpleObjectProperty<>();
    private final BooleanProperty inOverview = new SimpleBooleanProperty();
    private final Property<BrowserTransferProgress> progress = new SimpleObjectProperty<>();
    private final ObservableList<UUID> terminalRequests = FXCollections.observableArrayList();
    private FileSystem fileSystem;
    private BrowserFileSystemSavedState savedState;
    private BrowserFileSystemCache cache;

    public BrowserFileSystemTabModel(
            BrowserAbstractSessionModel<?> model,
            DataStoreEntryRef<? extends FileSystemStore> entry,
            SelectionMode selectionMode) {
        super(model, entry);
        this.inOverview.bind(Bindings.createBooleanBinding(
                () -> {
                    return currentPath.get() == null;
                },
                currentPath));
        fileList = new BrowserFileListModel(selectionMode, this);
    }

    @Override
    public Comp<?> comp() {
        return new BrowserFileSystemTabComp(this, true);
    }

    @Override
    public boolean canImmediatelyClose() {
        if (fileSystem == null
                || fileSystem.getShell().isEmpty()
                || !fileSystem.getShell().get().getLock().isLocked()) {
            return true;
        }

        return progress.getValue() == null || progress.getValue().done();
    }

    @Override
    public void init() throws Exception {
        BooleanScope.executeExclusive(busy, () -> {
            var fs = entry.getStore().createFileSystem();
            if (fs.getShell().isPresent()) {
                ProcessControlProvider.get().withDefaultScripts(fs.getShell().get());
            }
            fs.open();
            // Listen to kill after init as the shell might get killed during init for certain reasons
            if (fs.getShell().isPresent()) {
                fs.getShell().get().onKill(() -> {
                    browserModel.closeAsync(this);
                });
            }
            this.fileSystem = fs;

            this.cache = new BrowserFileSystemCache(this);
            for (BrowserAction b : BrowserAction.ALL) {
                b.init(this);
            }
        });
        this.savedState = BrowserFileSystemSavedState.loadForStore(this);
    }

    @Override
    public void close() {
        BooleanScope.executeExclusive(busy, () -> {
            if (fileSystem == null) {
                return;
            }

            var current = getCurrentDirectory();
            if (DataStorage.get().getStoreEntries().contains(getEntry().get())
                    && savedState != null
                    && current != null) {
                savedState.cd(current.getPath(), false);
                BrowserHistorySavedStateImpl.get()
                        .add(new BrowserHistorySavedState.Entry(getEntry().get().getUuid(), current.getPath()));
            }
            try {
                fileSystem.close();
            } catch (IOException e) {
                ErrorEvent.fromThrowable(e).handle();
            }
            fileSystem = null;
        });
    }

    private void startIfNeeded() throws Exception {
        if (fileSystem == null) {
            return;
        }

        var s = fileSystem.getShell();
        if (s.isPresent()) {
            s.get().start();
        }
    }

    public void withShell(FailableConsumer<ShellControl, Exception> c, boolean refresh) {
        ThreadHelper.runFailableAsync(() -> {
            if (fileSystem == null) {
                return;
            }

            BooleanScope.executeExclusive(busy, () -> {
                if (entry.getStore() instanceof ShellStore s) {
                    c.accept(fileSystem.getShell().orElseThrow());
                    if (refresh) {
                        refreshSync();
                    }
                }
            });
        });
    }

    @SneakyThrows
    public void refresh() {
        BooleanScope.executeExclusive(busy, () -> {
            cdSyncWithoutCheck(currentPath.get());
        });
    }

    public void refreshSync() throws Exception {
        cdSyncWithoutCheck(currentPath.get());
    }

    public FileEntry getCurrentParentDirectory() {
        var current = getCurrentDirectory();
        if (current == null) {
            return null;
        }

        var parent = FileNames.getParent(currentPath.get());
        if (parent == null) {
            return null;
        }

        return new FileEntry(fileSystem, parent, null, 0, null, FileKind.DIRECTORY);
    }

    public FileEntry getCurrentDirectory() {
        if (currentPath.get() == null) {
            return null;
        }

        if (fileSystem == null) {
            return null;
        }

        return new FileEntry(fileSystem, currentPath.get(), null, 0, null, FileKind.DIRECTORY);
    }

    public void cdAsync(String path) {
        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                cdSync(path);
            });
        });
    }

    public void cdSync(String path) {
        cdSyncOrRetry(path, false).ifPresent(s -> cdSyncOrRetry(s, false));
    }

    public Optional<String> cdSyncOrRetry(String path, boolean customInput) {
        if (Objects.equals(path, currentPath.get())) {
            return Optional.empty();
        }

        if (fileSystem == null) {
            return Optional.empty();
        }

        try {
            // Start shell in case we exited
            startIfNeeded();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.ofNullable(currentPath.get());
        }

        // Fix common issues with paths
        var adjustedPath = BrowserFileSystemHelper.adjustPath(this, path);
        if (!Objects.equals(path, adjustedPath)) {
            return Optional.of(adjustedPath);
        }

        // Evaluate optional expressions
        String evaluatedPath;
        try {
            evaluatedPath = BrowserFileSystemHelper.evaluatePath(this, adjustedPath);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.ofNullable(currentPath.get());
        }

        // Handle commands typed into navigation bar
        if (customInput
                && evaluatedPath != null
                && !evaluatedPath.isBlank()
                && !FileNames.isAbsolute(evaluatedPath)
                && fileSystem.getShell().isPresent()) {
            var directory = currentPath.get();
            var name = adjustedPath + " - " + entry.get().getName();
            ThreadHelper.runFailableAsync(() -> {
                if (ShellDialects.getStartableDialects().stream().anyMatch(dialect -> adjustedPath
                        .toLowerCase()
                        .startsWith(dialect.getExecutableName().toLowerCase()))) {
                    var uuid = UUID.randomUUID();
                    terminalRequests.add(uuid);
                    TerminalLauncher.open(
                            entry.getEntry(),
                            name,
                            directory,
                            fileSystem
                                    .getShell()
                                    .get()
                                    .singularSubShell(
                                            ShellOpenFunction.of(CommandBuilder.ofString(adjustedPath), false)),
                            uuid);
                } else {
                    var uuid = UUID.randomUUID();
                    terminalRequests.add(uuid);
                    TerminalLauncher.open(
                            entry.getEntry(),
                            name,
                            directory,
                            fileSystem.getShell().get().command(adjustedPath),
                            uuid);
                }
            });
            return Optional.ofNullable(currentPath.get());
        }

        // Evaluate optional links
        String resolvedPath;
        try {
            resolvedPath = BrowserFileSystemHelper.resolveDirectoryPath(this, evaluatedPath, customInput);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.ofNullable(currentPath.get());
        }

        if (!Objects.equals(path, resolvedPath)) {
            return Optional.ofNullable(resolvedPath);
        }

        try {
            BrowserFileSystemHelper.validateDirectoryPath(this, resolvedPath, customInput);
            cdSyncWithoutCheck(path);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.ofNullable(currentPath.get());
        }

        return Optional.empty();
    }

    private void cdSyncWithoutCheck(String path) throws Exception {
        if (fileSystem == null) {
            var fs = entry.getStore().createFileSystem();
            fs.open();
            this.fileSystem = fs;
        }

        // Assume that the path is normalized to improve performance!
        // path = FileSystemHelper.normalizeDirectoryPath(this, path);

        filter.setValue(null);
        savedState.cd(path, true);
        history.updateCurrent(path);
        currentPath.set(path);
        loadFilesSync(path);
    }

    public void withFiles(String dir, FailableConsumer<Stream<FileEntry>, Exception> consumer) throws Exception {
        BooleanScope.executeExclusive(busy, () -> {
            if (dir != null) {
                startIfNeeded();
                var fs = getFileSystem();
                if (fs == null) {
                    return;
                }

                var stream = fs.listFiles(dir);
                consumer.accept(stream);
            } else {
                consumer.accept(Stream.of());
            }
        });
    }

    private boolean loadFilesSync(String dir) {
        try {
            startIfNeeded();
            var fs = getFileSystem();
            if (dir != null && fs != null) {
                var stream = fs.listFiles(dir);
                fileList.setAll(stream);
            } else {
                fileList.setAll(Stream.of());
            }
            return true;
        } catch (Exception e) {
            fileList.setAll(Stream.of());
            ErrorEvent.fromThrowable(e).handle();
            return false;
        }
    }

    public void dropLocalFilesIntoAsync(FileEntry entry, List<Path> files) {
        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                startIfNeeded();
                var op = BrowserFileTransferOperation.ofLocal(
                        entry, files, BrowserFileTransferMode.COPY, true, progress::setValue);
                op.execute();
                refreshSync();
            });
        });
    }

    public void dropFilesIntoAsync(FileEntry target, List<FileEntry> files, BrowserFileTransferMode mode) {
        // We don't have to do anything in this case
        if (files.isEmpty()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                startIfNeeded();
                var op = new BrowserFileTransferOperation(target, files, mode, true, progress::setValue);
                op.execute();
                refreshSync();
            });
        });
    }

    public void createDirectoryAsync(String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        if (getCurrentDirectory() == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                startIfNeeded();
                var abs = FileNames.join(getCurrentDirectory().getPath(), name);
                if (fileSystem.directoryExists(abs)) {
                    throw ErrorEvent.expected(
                            new IllegalStateException(String.format("Directory %s already exists", abs)));
                }

                fileSystem.mkdirs(abs);
                refreshSync();
            });
        });
    }

    public void createLinkAsync(String linkName, String targetFile) {
        if (linkName == null || linkName.isBlank() || targetFile == null || targetFile.isBlank()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (getCurrentDirectory() == null) {
                    return;
                }

                startIfNeeded();
                var abs = FileNames.join(getCurrentDirectory().getPath(), linkName);
                fileSystem.symbolicLink(abs, targetFile);
                refreshSync();
            });
        });
    }

    public void runCommandAsync(CommandBuilder command, boolean refresh) {
        if (name == null || name.isBlank()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (getCurrentDirectory() == null) {
                    return;
                }

                fileSystem
                        .getShell()
                        .orElseThrow()
                        .command(command)
                        .withWorkingDirectory(getCurrentDirectory().getPath())
                        .execute();
                if (refresh) {
                    refreshSync();
                }
            });
        });
    }

    public void runAsync(FailableRunnable<Exception> r, boolean refresh) {
        if (name == null || name.isBlank()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (getCurrentDirectory() == null) {
                    return;
                }

                r.run();
                if (refresh) {
                    refreshSync();
                }
            });
        });
    }

    public void createFileAsync(String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (getCurrentDirectory() == null) {
                    return;
                }

                var abs = FileNames.join(getCurrentDirectory().getPath(), name);
                fileSystem.touch(abs);
                refreshSync();
            });
        });
    }

    public boolean isClosed() {
        return fileSystem == null;
    }

    public void initWithGivenDirectory(String dir) {
        cdSync(dir);
    }

    public void initWithDefaultDirectory() {
        savedState.cd(null, false);
        history.updateCurrent(null);
    }

    public void openTerminalAsync(String directory) {
        ThreadHelper.runFailableAsync(() -> {
            if (fileSystem == null) {
                return;
            }

            BooleanScope.executeExclusive(busy, () -> {
                if (fileSystem.getShell().isPresent()) {
                    var connection = fileSystem.getShell().get();
                    var name = (directory != null ? directory + " - " : "")
                            + entry.get().getName();
                    var uuid = UUID.randomUUID();
                    terminalRequests.add(uuid);
                    TerminalLauncher.open(entry.getEntry(), name, directory, connection, uuid);

                    // Restart connection as we will have to start it anyway, so we speed it up by doing it preemptively
                    startIfNeeded();
                }
            });
        });
    }

    public void backSync(int i) throws Exception {
        cdSync(history.back(i));
    }

    public void forthSync(int i) throws Exception {
        cdSync(history.forth(i));
    }

    @Getter
    public enum SelectionMode {
        SINGLE_FILE(false, true, false),
        MULTIPLE_FILE(true, true, false),
        SINGLE_DIRECTORY(false, false, true),
        MULTIPLE_DIRECTORY(true, false, true),
        ALL(true, true, true);

        private final boolean multiple;
        private final boolean acceptsFiles;
        private final boolean acceptsDirectories;

        SelectionMode(boolean multiple, boolean acceptsFiles, boolean acceptsDirectories) {
            this.multiple = multiple;
            this.acceptsFiles = acceptsFiles;
            this.acceptsDirectories = acceptsDirectories;
        }
    }
}