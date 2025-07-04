package io.xpipe.app.browser.file;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.browser.BrowserAbstractSessionModel;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.BrowserStoreSessionTab;
import io.xpipe.app.browser.action.impl.TransferFilesActionProvider;
import io.xpipe.app.browser.menu.BrowserMenuItemProvider;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.WrapperFileSystem;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.terminal.*;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.*;
import io.xpipe.core.store.*;
import io.xpipe.core.util.FailableConsumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public final class BrowserFileSystemTabModel extends BrowserStoreSessionTab<FileSystemStore> {

    private final Property<String> filter = new SimpleStringProperty();
    private final BrowserFileListModel fileList;
    private final ReadOnlyObjectWrapper<FilePath> currentPath = new ReadOnlyObjectWrapper<>();
    private final BrowserFileSystemHistory history = new BrowserFileSystemHistory();
    private final BooleanProperty inOverview = new SimpleBooleanProperty();
    private final Property<BrowserTransferProgress> progress = new SimpleObjectProperty<>();
    private final ObservableList<UUID> terminalRequests = FXCollections.observableArrayList();
    private final BooleanProperty transferCancelled = new SimpleBooleanProperty();

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

    public Optional<FileEntry> findFile(FilePath path) {
        return getFileList().getAll().getValue().stream()
                .filter(browserEntry -> browserEntry.getFileName().equals(path.toString())
                        || browserEntry.getRawFileEntry().getPath().equals(path))
                .findFirst()
                .map(browserEntry -> browserEntry.getRawFileEntry());
    }

    @Override
    public Comp<?> comp() {
        return new BrowserFileSystemTabComp(this, true);
    }

    @Override
    public boolean canImmediatelyClose() {
        if (fileSystem.getShell().isEmpty()
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
                var originalFs = fs;
                fs = new WrapperFileSystem(
                        originalFs, () -> originalFs.getShell().get().isRunning(true));
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
            for (var a : ActionProvider.ALL) {
                if (a instanceof BrowserMenuItemProvider ba) {
                    ba.init(this);
                }
            }
        });
        this.savedState = BrowserFileSystemSavedState.loadForStore(this);
    }

    @Override
    public void close() {
        BooleanScope.executeExclusive(busy, () -> {
            var current = currentPath.getValue();
            // We might close this after storage shutdown
            // If this entry does not exist, it's not that bad if we save it anyway
            if (
            //                    DataStorage.get() != null
            //                    && DataStorage.get().getStoreEntries().contains(getEntry().get())
            savedState != null && current != null) {
                savedState.cd(current, false);
                BrowserHistorySavedStateImpl.get()
                        .add(new BrowserHistorySavedState.Entry(getEntry().get().getUuid(), current));
                BrowserHistorySavedStateImpl.get().save();
            }
            try {
                fileSystem.close();
            } catch (IOException e) {
                ErrorEventFactory.fromThrowable(e).handle();
            }
        });
    }

    private void startIfNeeded() throws Exception {
        var s = fileSystem.getShell();
        if (s.isPresent()) {
            s.get().start();
        }
    }

    public void killTransfer() {
        transferCancelled.set(true);
    }

    public void refreshSync() {
        cdSyncWithoutCheck(currentPath.get());
    }

    public void refreshBrowserEntriesSync(List<BrowserEntry> entries) throws Exception {
        refreshFileEntriesSync(
                entries.stream().map(BrowserEntry::getRawFileEntry).collect(Collectors.toList()));
    }

    public void refreshFileEntriesSync(List<FileEntry> entries) throws Exception {
        if (fileList.getAll().getValue().size() < 10) {
            refreshSync();
            return;
        }

        if (entries.size() > 10 && fileList.getAll().getValue().size() < 100) {
            refreshSync();
            return;
        }

        for (var e : entries) {
            var refresh = fileSystem.getFileInfo(e.getPath());
            fileList.updateEntry(e.getPath(), refresh.orElse(null));
        }
    }

    public FileEntry getCurrentParentDirectory() {
        if (currentPath.get() == null) {
            return null;
        }

        var parent = currentPath.get().getParent();
        if (parent == null) {
            return null;
        }

        return new FileEntry(fileSystem, parent, null, null, null, FileKind.DIRECTORY);
    }

    public FileEntry getCurrentDirectory() {
        if (currentPath.get() == null) {
            return null;
        }

        return new FileEntry(fileSystem, currentPath.get(), null, null, null, FileKind.DIRECTORY);
    }

    public void cdAsync(FilePath path) {
        cdAsync(path != null ? path.toString() : null);
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

    private boolean shouldLaunchSplitTerminal() {
        if (!AppPrefs.get().enableTerminalDocking().get()) {
            return false;
        }

        if (OsType.getLocal() != OsType.WINDOWS) {
            return false;
        }

        if (AppMainWindow.getInstance().getStage().getWidth() <= 1380) {
            return false;
        }

        var term = AppPrefs.get().terminalType().getValue();
        if (term == null || term.getOpenFormat() == TerminalOpenFormat.TABBED) {
            return false;
        }

        if (!(browserModel instanceof BrowserFullSessionModel f)) {
            return false;
        }

        // Check if the right side is already occupied
        var existingSplit = f.getEffectiveRightTab().getValue();
        if (existingSplit == this) {
            return false;
        }
        if (existingSplit != null && !(existingSplit instanceof BrowserTerminalDockTabModel)) {
            return false;
        }

        return true;
    }

    public Optional<String> cdSyncOrRetry(String path, boolean customInput) {
        var cps = currentPath.get() != null ? currentPath.get().toString() : null;
        if (Objects.equals(path, cps)) {
            return Optional.empty();
        }

        try {
            // Start shell in case we exited
            startIfNeeded();
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return Optional.ofNullable(cps);
        }

        if (path == null) {
            currentPath.set(null);
            return Optional.empty();
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
            ErrorEventFactory.fromThrowable(ex).handle();
            return Optional.ofNullable(cps);
        }

        if (evaluatedPath == null) {
            return Optional.empty();
        }

        // Handle commands typed into navigation bar
        if (customInput
                && !evaluatedPath.isBlank()
                && !FilePath.of(evaluatedPath).isAbsolute()
                && fileSystem.getShell().isPresent()) {
            var directory = currentPath.get();
            var name = adjustedPath + " - " + entry.get().getName();
            ThreadHelper.runFailableAsync(() -> {
                if (ShellDialects.getStartableDialects().stream().anyMatch(dialect -> adjustedPath
                        .toLowerCase()
                        .startsWith(dialect.getExecutableName().toLowerCase()))) {
                    var sub = fileSystem.getShell().get().subShell();
                    var open = new ShellOpenFunction() {

                        @Override
                        public CommandBuilder prepareWithoutInitCommand() {
                            return CommandBuilder.ofString(adjustedPath);
                        }

                        @Override
                        public CommandBuilder prepareWithInitCommand(@NonNull String command) {
                            return CommandBuilder.ofString(command);
                        }
                    };
                    sub.setDumbOpen(open);
                    sub.setTerminalOpen(open);
                    openTerminalAsync(name, directory, sub, true);
                } else {
                    var cc = fileSystem.getShell().get().command(adjustedPath);
                    openTerminalAsync(name, directory, cc, true);
                }
            });
            return Optional.ofNullable(cps);
        }

        // Evaluate optional links
        FilePath resolvedPath;
        try {
            resolvedPath = BrowserFileSystemHelper.resolveDirectoryPath(this, FilePath.of(evaluatedPath), customInput);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return Optional.ofNullable(cps);
        }

        if (!Objects.equals(path, resolvedPath.toString())) {
            return Optional.of(resolvedPath.toString());
        }

        try {
            BrowserFileSystemHelper.validateDirectoryPath(this, resolvedPath, true);
            cdSyncWithoutCheck(resolvedPath);
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).handle();
            return Optional.ofNullable(cps);
        }

        return Optional.empty();
    }

    private void cdSyncWithoutCheck(FilePath path) {

        // Assume that the path is normalized to improve performance!
        // path = FileSystemHelper.normalizeDirectoryPath(this, path);

        filter.setValue(null);
        savedState.cd(path, true);
        history.updateCurrent(path);
        currentPath.set(path);
        loadFilesSync(path);
    }

    public void withFiles(FilePath dir, FailableConsumer<Stream<FileEntry>, Exception> consumer) throws Exception {
        BooleanScope.executeExclusive(busy, () -> {
            if (dir != null) {
                startIfNeeded();
                var fs = getFileSystem();

                var stream = fs.listFiles(dir);
                consumer.accept(stream);
            } else {
                consumer.accept(Stream.of());
            }
        });
    }

    private boolean loadFilesSync(FilePath dir) {
        try {
            startIfNeeded();
            var fs = getFileSystem();
            if (dir != null) {
                var stream = fs.listFiles(dir);
                fileList.setAll(stream);
            } else {
                fileList.setAll(Stream.of());
            }
            return true;
        } catch (Exception e) {
            fileList.setAll(Stream.of());
            ErrorEventFactory.fromThrowable(e).handle();
            return false;
        }
    }

    public void dropLocalFilesIntoAsync(FileEntry entry, List<Path> files) {
        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                startIfNeeded();
                var op = BrowserFileTransferOperation.ofLocal(
                        entry, files, BrowserFileTransferMode.COPY, true, progress::setValue, transferCancelled);
                var action = TransferFilesActionProvider.Action.builder()
                        .operation(op)
                        .target(this.entry.asNeeded())
                        .build();
                action.executeSync();
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
                startIfNeeded();
                var op = new BrowserFileTransferOperation(
                        target, files, mode, true, progress::setValue, transferCancelled);
                var action = TransferFilesActionProvider.Action.builder()
                        .operation(op)
                        .target(entry.asNeeded())
                        .build();
                action.executeSync();
                refreshSync();
            });
        });
    }

    public boolean isClosed() {
        return false;
    }

    public void initWithGivenDirectory(FilePath dir) {
        cdSync(dir != null ? dir.toString() : null);
    }

    public void initWithDefaultDirectory() {
        savedState.cd(null, false);
        history.updateCurrent(null);
    }

    public void openTerminalAsync(
            String name, FilePath directory, ProcessControl processControl, boolean dockIfPossible) {
        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                openTerminalSync(name, directory, processControl, dockIfPossible);
            });
        });
    }

    public void openTerminalSync(String name, FilePath directory, ProcessControl processControl, boolean dockIfPossible)
            throws Exception {
        var dock = shouldLaunchSplitTerminal() && dockIfPossible;
        var uuid = UUID.randomUUID();
        terminalRequests.add(uuid);
        if (dock
                && browserModel instanceof BrowserFullSessionModel fullSessionModel
                && !(fullSessionModel.getSplits().get(this) instanceof BrowserTerminalDockTabModel)) {
            fullSessionModel.splitTab(this, new BrowserTerminalDockTabModel(browserModel, this, terminalRequests));
        }
        TerminalLauncher.open(entry.get(), name, directory, processControl, uuid, !dock);

        // Restart connection as we will have to start it anyway, so we speed it up by doing it preemptively
        startIfNeeded();
    }

    public void backSync(int i) {
        var b = history.back(i);
        if (b != null) {
            cdSync(b.toString());
        }
    }

    public void forthSync(int i) {
        var f = history.forth(i);
        if (f != null) {
            cdSync(f.toString());
        }
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
