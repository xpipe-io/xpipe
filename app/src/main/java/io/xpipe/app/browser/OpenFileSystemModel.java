package io.xpipe.app.browser;

import io.xpipe.app.browser.action.BrowserAction;
import io.xpipe.app.comp.base.ModalOverlayComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.TerminalHelper;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.*;
import io.xpipe.core.store.*;
import io.xpipe.core.util.FailableConsumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import lombok.Getter;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
public final class OpenFileSystemModel {

    private final DataStoreEntryRef<? extends FileSystemStore> entry;
    private FileSystem fileSystem;
    private final Property<String> filter = new SimpleStringProperty();
    private final BrowserFileListModel fileList;
    private final ReadOnlyObjectWrapper<String> currentPath = new ReadOnlyObjectWrapper<>();
    private final OpenFileSystemHistory history = new OpenFileSystemHistory();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final BrowserModel browserModel;
    private OpenFileSystemSavedState savedState;
    private OpenFileSystemCache cache;
    private final Property<ModalOverlayComp.OverlayContent> overlay = new SimpleObjectProperty<>();
    private final BooleanProperty inOverview = new SimpleBooleanProperty();
    private final String name;
    private final String tooltip;
    private boolean local;
    private int customScriptsStartIndex;

    public OpenFileSystemModel(BrowserModel browserModel, DataStoreEntryRef<? extends FileSystemStore> entry) {
        this.browserModel = browserModel;
        this.entry = entry;
        this.name = DataStorage.get().getStoreDisplayName(entry.get());
        this.tooltip = DataStorage.get().getId(entry.getEntry()).toString();
        this.inOverview.bind(Bindings.createBooleanBinding(
                () -> {
                    return currentPath.get() == null;
                },
                currentPath));
        fileList = new BrowserFileListModel(this);
    }

    public void withShell(FailableConsumer<ShellControl, Exception> c, boolean refresh) {
        ThreadHelper.runFailableAsync(() -> {
            if (fileSystem == null) {
                return;
            }

            BooleanScope.execute(busy, () -> {
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
        BooleanScope.execute(busy, () -> {
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

        return new FileSystem.FileEntry(fileSystem, parent, null, false, false, 0, null, FileKind.DIRECTORY);
    }

    public FileSystem.FileEntry getCurrentDirectory() {
        if (currentPath.get() == null) {
            return null;
        }

        if (fileSystem == null) {
            return null;
        }

        return new FileSystem.FileEntry(fileSystem, currentPath.get(), null, false, false, 0, null, FileKind.DIRECTORY);
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

    public Optional<String> cdSyncOrRetry(String path, boolean allowCommands) {
        if (Objects.equals(path, currentPath.get())) {
            return Optional.empty();
        }

        if (fileSystem == null) {
            return Optional.empty();
        }

        // Fix common issues with paths
        var adjustedPath = FileSystemHelper.adjustPath(this, path);
        if (!Objects.equals(path, adjustedPath)) {
            return Optional.of(adjustedPath);
        }

        // Evaluate optional expressions
        String evaluatedPath;
        try {
            evaluatedPath = FileSystemHelper.evaluatePath(this, adjustedPath);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.ofNullable(currentPath.get());
        }

        // Handle commands typed into navigation bar
        if (allowCommands
                && evaluatedPath != null
                && !evaluatedPath.isBlank()
                && !FileNames.isAbsolute(evaluatedPath)
                && fileSystem.getShell().isPresent()) {
            var directory = currentPath.get();
            var name = adjustedPath + " - " + entry.get().getName();
            ThreadHelper.runFailableAsync(() -> {
                if (ShellDialects.getStartableDialects().stream().anyMatch(dialect -> adjustedPath.startsWith(dialect.getOpenCommand()))) {
                    TerminalHelper.open(
                            entry.getEntry(),
                            name,
                            fileSystem
                                    .getShell()
                                    .get()
                                    .subShell(adjustedPath)
                                    .withInitSnippet(new SimpleScriptSnippet(
                                            fileSystem
                                                    .getShell()
                                                    .get()
                                                    .getShellDialect()
                                                    .getCdCommand(currentPath.get()),
                                            ScriptSnippet.ExecutionType.BOTH)));
                } else {
                    TerminalHelper.open(
                            entry.getEntry(),
                            name,
                            fileSystem.getShell().get().command(adjustedPath).withWorkingDirectory(directory));
                }
            });
            return Optional.ofNullable(currentPath.get());
        }

        // Evaluate optional links
        String resolvedPath;
        try {
            resolvedPath = FileSystemHelper.resolveDirectoryPath(this, evaluatedPath);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return Optional.ofNullable(currentPath.get());
        }

        if (!Objects.equals(path, resolvedPath)) {
            return Optional.ofNullable(resolvedPath);
        }

        try {
            FileSystemHelper.validateDirectoryPath(this, resolvedPath);
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
        savedState.cd(path);
        history.updateCurrent(path);
        currentPath.set(path);
        loadFilesSync(path);
    }

    private boolean loadFilesSync(String dir) {
        try {
            if (dir != null) {
                var stream = getFileSystem().listFiles(dir);
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

    public void dropLocalFilesIntoAsync(FileSystem.FileEntry entry, List<Path> files) {
        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.execute(busy, () -> {
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
        // We don't have to do anything in this case
        if (files.isEmpty()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                var same = files.get(0).getFileSystem().equals(target.getFileSystem());
                if (same && !explicitCopy) {
                    if (!BrowserAlerts.showMoveAlert(files, target)) {
                        return;
                    }
                }

                FileSystemHelper.dropFilesInto(target, files, explicitCopy);
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
            BooleanScope.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                var abs = FileNames.join(getCurrentDirectory().getPath(), name);
                if (fileSystem.directoryExists(abs)) {
                    throw ErrorEvent.unreportable(new IllegalStateException(String.format("Directory %s already exists", abs)));
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
            BooleanScope.execute(busy, () -> {
                if (fileSystem == null) {
                    return;
                }

                if (getCurrentDirectory() == null) {
                    return;
                }

                var abs = FileNames.join(getCurrentDirectory().getPath(), linkName);
                fileSystem.symbolicLink(abs, targetFile);
                refreshSync();
            });
        });
    }

    public void createFileAsync(String name) {
        if (name == null || name.isBlank()) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.execute(busy, () -> {
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
    }

    public boolean isClosed() {
        return fileSystem == null;
    }

    public void initFileSystem() throws Exception {
        BooleanScope.execute(busy, () -> {
            var fs = entry.getStore().createFileSystem();
            if (fs.getShell().isPresent()) {
                this.customScriptsStartIndex = fs.getShell().get().getInitCommands().size();
                ProcessControlProvider.get().withDefaultScripts(fs.getShell().get());
            }
            fs.open();
            this.fileSystem = fs;
            this.local = fs.getShell()
                    .map(shellControl -> shellControl.hasLocalSystemAccess())
                    .orElse(false);

            this.cache = new OpenFileSystemCache(this);
            for (BrowserAction b : BrowserAction.ALL) {
                b.init(this);
            }
        });
    }

    public void initWithGivenDirectory(String dir) throws Exception {
        cdSyncWithoutCheck(dir);
    }

    public void initWithDefaultDirectory() {
        savedState.cd(null);
        history.updateCurrent(null);
    }

    void initSavedState() {
        this.savedState = OpenFileSystemSavedState.loadForStore(this);
    }

    public void openTerminalAsync(String directory) {
        ThreadHelper.runFailableAsync(() -> {
            if (fileSystem == null) {
                return;
            }

            BooleanScope.execute(busy, () -> {
                if (fileSystem.getShell().isPresent()) {
                    var connection = fileSystem.getShell().get();
                    var snippet = directory != null ? new SimpleScriptSnippet(connection.getShellDialect().getCdCommand(directory),
                            ScriptSnippet.ExecutionType.BOTH) : null;
                    if (snippet != null) {
                        connection.getInitCommands().add(customScriptsStartIndex,snippet);
                    }

                    try {
                        var name = (directory != null ? directory + " - " : "") + entry.get().getName();
                        TerminalHelper.open(entry.getEntry(), name, connection);

                        // Restart connection as we will have to start it anyway, so we speed it up by doing it preemptively
                        connection.start();
                    } finally {
                        connection.getInitCommands().remove(snippet);
                    }
                }
            });
        });
    }

    public void backSync(int i) throws Exception {
        cdSyncWithoutCheck(history.back(i));
    }

    public void forthSync(int i) throws Exception {
        cdSyncWithoutCheck(history.forth(i));
    }
}
