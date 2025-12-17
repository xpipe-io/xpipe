package io.xpipe.app.browser.file;

import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileKind;
import io.xpipe.app.ext.FileSystem;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;

import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BrowserFileTransferOperation {

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    @Getter
    private final FileEntry target;

    @Getter
    private final List<FileEntry> files;

    private final BrowserFileTransferMode transferMode;
    private final boolean checkConflicts;
    private final Consumer<BrowserTransferProgress> progress;
    private final BooleanProperty cancelled;
    BrowserDialogs.FileConflictChoice lastConflictChoice;

    public BrowserFileTransferOperation(
            FileEntry target,
            List<FileEntry> files,
            BrowserFileTransferMode transferMode,
            boolean checkConflicts,
            Consumer<BrowserTransferProgress> progress,
            BooleanProperty cancelled) {
        this.target = target;
        this.files = files;
        this.transferMode = transferMode;
        this.checkConflicts = checkConflicts;
        this.progress = progress;
        this.cancelled = cancelled;
    }

    public static BrowserFileTransferOperation ofLocal(
            FileEntry target,
            List<Path> files,
            BrowserFileTransferMode transferMode,
            boolean checkConflicts,
            Consumer<BrowserTransferProgress> progress,
            BooleanProperty cancelled) {
        var entries = files.stream()
                .map(path -> {
                    if (!Files.exists(path)) {
                        return null;
                    }

                    try {
                        return BrowserLocalFileSystem.getLocalFileEntry(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(entry -> entry != null)
                .toList();
        return new BrowserFileTransferOperation(target, entries, transferMode, checkConflicts, progress, cancelled);
    }

    private void reinitFileSystemsIfNeeded() throws Exception {
        getFiles().getFirst().getFileSystem().reinitIfNeeded();
        getTarget().getFileSystem().reinitIfNeeded();
    }

    private void updateProgress(BrowserTransferProgress progress) {
        this.progress.accept(progress);
    }

    private BrowserDialogs.FileConflictChoice handleChoice(FileSystem fileSystem, FilePath target, boolean multiple)
            throws Exception {
        if (lastConflictChoice == BrowserDialogs.FileConflictChoice.CANCEL) {
            return BrowserDialogs.FileConflictChoice.CANCEL;
        }

        if (lastConflictChoice == BrowserDialogs.FileConflictChoice.REPLACE_ALL) {
            return BrowserDialogs.FileConflictChoice.REPLACE;
        }

        if (lastConflictChoice == BrowserDialogs.FileConflictChoice.RENAME_ALL) {
            return BrowserDialogs.FileConflictChoice.RENAME;
        }

        if (fileSystem.fileExists(target)) {
            if (lastConflictChoice == BrowserDialogs.FileConflictChoice.SKIP_ALL) {
                return BrowserDialogs.FileConflictChoice.SKIP;
            }

            var choice = BrowserDialogs.showFileConflictDialog(target, multiple);
            if (choice == BrowserDialogs.FileConflictChoice.CANCEL) {
                lastConflictChoice = BrowserDialogs.FileConflictChoice.CANCEL;
                return BrowserDialogs.FileConflictChoice.CANCEL;
            }

            if (choice == BrowserDialogs.FileConflictChoice.SKIP) {
                return BrowserDialogs.FileConflictChoice.SKIP;
            }

            if (choice == BrowserDialogs.FileConflictChoice.SKIP_ALL) {
                lastConflictChoice = BrowserDialogs.FileConflictChoice.SKIP_ALL;
                return BrowserDialogs.FileConflictChoice.SKIP;
            }

            if (choice == BrowserDialogs.FileConflictChoice.REPLACE_ALL) {
                lastConflictChoice = BrowserDialogs.FileConflictChoice.REPLACE_ALL;
                return BrowserDialogs.FileConflictChoice.REPLACE;
            }

            if (choice == BrowserDialogs.FileConflictChoice.RENAME_ALL) {
                lastConflictChoice = BrowserDialogs.FileConflictChoice.RENAME_ALL;
                return BrowserDialogs.FileConflictChoice.RENAME;
            }

            return choice;
        }
        return BrowserDialogs.FileConflictChoice.REPLACE;
    }

    private boolean cancelled() {
        return cancelled.get() || AppOperationMode.isInShutdown();
    }

    public boolean isMove() {
        if (files.isEmpty()) {
            return false;
        }

        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        var doesMove = transferMode == BrowserFileTransferMode.MOVE
                || (same && transferMode == BrowserFileTransferMode.NORMAL);
        return doesMove;
    }

    public void execute() throws Exception {
        if (files.isEmpty()) {
            updateProgress(null);
            return;
        }

        reinitFileSystemsIfNeeded();

        if (target.getKind() != FileKind.DIRECTORY) {
            throw new IllegalStateException("Target " + target.getPath() + " is not a directory");
        }

        BrowserFileSystemHelper.validateDirectoryPath(target.getFileSystem(), target.getPath(), true);

        cancelled.set(false);

        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        var doesMove = transferMode == BrowserFileTransferMode.MOVE
                || (same && transferMode == BrowserFileTransferMode.NORMAL);
        try {
            for (var file : files) {
                if (cancelled()) {
                    break;
                }

                if (same) {
                    handleSingleOnSameFileSystem(file);
                } else {
                    // Transfers might change the working directory
                    var currentDir = file.getFileSystem().pwd();
                    handleSingleAcrossFileSystems(file);

                    // Expect a kill
                    if (currentDir.isPresent() && !file.getFileSystem().requiresReinit()) {
                        file.getFileSystem().cd(currentDir.get());
                    }
                }
            }

            if (!same && doesMove) {
                for (var file : files) {
                    if (cancelled()) {
                        break;
                    }

                    deleteSingle(file);
                }
            }
        } finally {
            updateProgress(null);
        }
    }

    private void handleSingleOnSameFileSystem(FileEntry source) throws Exception {
        // Prevent dropping files into itself
        if ((source.getKind() == FileKind.DIRECTORY && source.getPath().equals(target.getPath()))
                || (source.getKind() != FileKind.DIRECTORY
                        && source.getPath().getParent().equals(target.getPath()))) {
            return;
        }

        var sourceFile = source.getPath();
        var targetFile = target.getPath().join(sourceFile.getFileName());

        if (sourceFile.equals(targetFile)) {
            // Duplicate file by renaming it
            targetFile = BrowserFileDuplicates.renameFileDuplicate(
                    target.getFileSystem(), targetFile, source.getKind() == FileKind.DIRECTORY);
        }

        if (source.getKind() == FileKind.DIRECTORY && target.getFileSystem().directoryExists(targetFile)) {
            throw ErrorEventFactory.expected(
                    new IllegalArgumentException("Target directory " + targetFile + " does already exist"));
        }

        if (checkConflicts) {
            var fileConflictChoice = handleChoice(target.getFileSystem(), targetFile, files.size() > 1);
            if (fileConflictChoice == BrowserDialogs.FileConflictChoice.SKIP
                    || fileConflictChoice == BrowserDialogs.FileConflictChoice.CANCEL) {
                return;
            }

            if (fileConflictChoice == BrowserDialogs.FileConflictChoice.RENAME) {
                targetFile = BrowserFileDuplicates.renameFileDuplicate(
                        target.getFileSystem(), targetFile, source.getKind() == FileKind.DIRECTORY);
            }
        }

        var doesMove = transferMode == BrowserFileTransferMode.MOVE || transferMode == BrowserFileTransferMode.NORMAL;
        if (doesMove) {
            target.getFileSystem().move(sourceFile, targetFile);
        } else {
            target.getFileSystem().copy(sourceFile, targetFile);
        }
    }

    private void handleSingleAcrossFileSystems(FileEntry source) throws Exception {
        var flatFiles = new LinkedHashMap<FileEntry, FilePath>();

        // Prevent dropping directory into itself
        if (source.getFileSystem().equals(target.getFileSystem())
                && source.getPath().startsWith(target.getPath())) {
            return;
        }

        AtomicLong totalSize = new AtomicLong();
        if (source.getKind() == FileKind.DIRECTORY) {
            // Source might have been deleted meanwhile
            var exists = source.getFileSystem().directoryExists(source.getPath());
            if (!exists) {
                progress.accept(BrowserTransferProgress.finished(source.getName(), 0));
                return;
            }

            var directoryName = source.getPath().getFileName();
            if (!source.getPath().isRoot()) {
                flatFiles.put(source, FilePath.of(directoryName));
            }

            var baseRelative = source.getPath().getParent().toDirectory();
            source.getFileSystem().traverseFilesRecursively(source.getFileSystem(), source.getPath(), fileEntry -> {
                if (cancelled()) {
                    progress.accept(BrowserTransferProgress.finished(source.getName() + " ...", totalSize.get()));
                    return false;
                }

                var rel = fileEntry.getPath().relativize(baseRelative).toUnix();
                flatFiles.put(fileEntry, rel);
                if (fileEntry.getKind() == FileKind.FILE) {
                    // This one is up-to-date and does not need to be recalculated
                    // If we don't have a size, it doesn't matter that much as the total size is only for display
                    totalSize.addAndGet(fileEntry.getFileSizeLong().orElse(0));
                    progress.accept(new BrowserTransferProgress(source.getName() + " ...", 0, totalSize.get()));
                }
                return true;
            });
        } else if (source.getKind() == FileKind.FILE) {
            // Source might have been deleted meanwhile
            var exists = source.getFileSystem().fileExists(source.getPath());
            if (!exists) {
                progress.accept(BrowserTransferProgress.finished(source.getName(), 0));
                return;
            }

            flatFiles.put(source, FilePath.of(source.getPath().getFileName()));
            // If we don't have a size, it doesn't matter that much as the total size is only for display
            totalSize.addAndGet(source.getFileSizeLong().orElse(0));
        } else {
            // Unsupported type, e.g. a socket
            progress.accept(BrowserTransferProgress.finished(source.getName(), 0));
            return;
        }

        var originalSourceFs = flatFiles.keySet().iterator().next().getFileSystem();
        if (!flatFiles.keySet().stream()
                .allMatch(fileEntry -> fileEntry.getFileSystem().equals(originalSourceFs))) {
            throw new IllegalArgumentException("Mixed source file systems");
        }

        var optimizedSourceFs = originalSourceFs.createTransferOptimizedFileSystem();
        var targetFs = target.getFileSystem().createTransferOptimizedFileSystem();

        try {
            AtomicLong transferred = new AtomicLong();
            for (var e : flatFiles.entrySet()) {
                if (cancelled()) {
                    return;
                }

                var sourceFile = e.getKey();
                var fixedRelPath = targetFs.makeFileSystemCompatible(e.getValue());
                var targetFile = target.getPath().join(fixedRelPath.toString());
                if (sourceFile.getFileSystem().equals(targetFs)) {
                    throw new IllegalStateException();
                }

                if (sourceFile.getKind() == FileKind.DIRECTORY) {
                    targetFs.mkdirs(targetFile);
                } else if (sourceFile.getKind() == FileKind.FILE) {
                    if (checkConflicts) {
                        var fileConflictChoice =
                                handleChoice(targetFs, targetFile, files.size() > 1 || flatFiles.size() > 1);
                        if (fileConflictChoice == BrowserDialogs.FileConflictChoice.SKIP
                                || fileConflictChoice == BrowserDialogs.FileConflictChoice.CANCEL) {
                            continue;
                        }

                        if (fileConflictChoice == BrowserDialogs.FileConflictChoice.RENAME) {
                            targetFile = BrowserFileDuplicates.renameFileDuplicate(targetFs, targetFile, false);
                        }
                    }

                    transfer(sourceFile.getPath(), optimizedSourceFs, targetFile, targetFs, transferred, totalSize);
                }
            }
        } finally {
            updateProgress(BrowserTransferProgress.finished(source.getName(), totalSize.get()));

            if (optimizedSourceFs != originalSourceFs) {
                optimizedSourceFs.close();
            }
            if (target.getFileSystem() != targetFs) {
                targetFs.close();
            }
        }
    }

    private void transfer(
            FilePath sourceFile,
            FileSystem sourceFs,
            FilePath targetFile,
            FileSystem targetFs,
            AtomicLong transferred,
            AtomicLong totalSize)
            throws Exception {
        if (cancelled()) {
            return;
        }

        var fileSize = sourceFs.getFileSize(sourceFile);

        // TODO: this is not ready yet
//        updateProgress(new BrowserTransferProgress(sourceFile.getFileName(), 0, 0));
//        if (targetFs.writeInstantIfPossible(sourceFs, sourceFile, targetFile) || sourceFs.readInstantIfPossible(sourceFile, targetFs, targetFile)) {
//            updateProgress(BrowserTransferProgress.finished(sourceFile.getFileName(), fileSize));
//            return;
//        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {

            // Read the first few bytes to figure out possible command failure early
            // before creating the output stream
            inputStream = new BufferedInputStream(sourceFs.openInput(sourceFile), 1024);
            inputStream.mark(1024);
            var streamStart = new byte[1024];
            var streamStartLength = inputStream.read(streamStart, 0, 1024);
            if (streamStartLength < 1024) {
                inputStream.close();
                inputStream = new ByteArrayInputStream(streamStart, 0, streamStartLength);
            } else {
                inputStream.reset();
            }

            outputStream = targetFs.openOutput(targetFile, fileSize);
            transferFile(sourceFile, inputStream, outputStream, transferred, totalSize, fileSize);
        } catch (Exception ex) {
            // Mark progress as finished to reset any progress display
            updateProgress(BrowserTransferProgress.finished(sourceFile.getFileName(), transferred.get()));

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception om) {
                    // This is expected as the process control has to be killed
                    // When calling close, it will throw an exception when it has to kill
                    ErrorEventFactory.fromThrowable(om).expected().omit().handle();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception om) {
                    // This is expected as the process control has to be killed
                    // When calling close, it will throw an exception when it has to kill
                    ErrorEventFactory.fromThrowable(om).expected().omit().handle();
                }
            }
            throw ex;
        }

        // If we receive a cancel while we are closing, there's a good chance that the close is stuck
        // Then, we just straight up kill the shells
        ChangeListener<Boolean> closeCancelListener = (observableValue, oldValue, newValue) -> {
            if (!newValue) {
                return;
            }

            sourceFs.kill();
            targetFs.kill();
        };
        cancelled.addListener(closeCancelListener);

        Exception exception = null;
        try {
            inputStream.close();
        } catch (Exception om) {
            exception = om;
        }
        try {
            outputStream.close();
        } catch (Exception om) {
            if (exception != null) {
                exception.addSuppressed(om);
            } else {
                exception = om;
            }
        }

        cancelled.removeListener(closeCancelListener);

        if (exception != null) {
            ErrorEventFactory.preconfigure(ErrorEventFactory.fromThrowable(exception)
                    .reportable(!cancelled())
                    .omitted(cancelled()));
            throw exception;
        }
    }

    private void deleteSingle(FileEntry source) throws Exception {
        source.getFileSystem().delete(source.getPath());
    }

    private void transferFile(
            FilePath sourceFile,
            InputStream inputStream,
            OutputStream outputStream,
            AtomicLong transferred,
            AtomicLong total,
            long expectedFileSize)
            throws Exception {
        // Initialize progress immediately prior to reading anything
        updateProgress(new BrowserTransferProgress(sourceFile.getFileName(), transferred.get(), total.get()));

        var killStreams = new AtomicBoolean(false);
        var exception = new AtomicReference<Exception>();
        var readCount = new AtomicLong();
        var thread = ThreadHelper.createPlatformThread("transfer", true, () -> {
            try {
                var bs = (int) Math.min(DEFAULT_BUFFER_SIZE, expectedFileSize);
                byte[] buffer = new byte[bs];
                int read;
                while ((read = inputStream.read(buffer, 0, bs)) > 0) {
                    if (cancelled()) {
                        killStreams.set(true);
                        break;
                    }

                    if (!checkTransferValidity()) {
                        killStreams.set(true);
                        break;
                    }

                    outputStream.write(buffer, 0, read);
                    transferred.addAndGet(read);
                    readCount.addAndGet(read);
                    updateProgress(
                            new BrowserTransferProgress(sourceFile.getFileName(), transferred.get(), total.get()));
                }

                outputStream.flush();
                inputStream.transferTo(OutputStream.nullOutputStream());

                var incomplete = !killStreams.get() && readCount.get() < expectedFileSize;
                if (incomplete) {
                    throw new IOException("Source file " + sourceFile + " input size mismatch: Expected "
                            + expectedFileSize + " but got " + readCount.get() + ". Did the source file get updated?");
                }
            } catch (Exception ex) {
                exception.set(ex);
                killStreams.set(true);
            }
        });

        thread.start();
        while (true) {
            var alive = thread.isAlive();
            var cancelled = cancelled();

            if (cancelled) {
                killStreams(thread, readCount, false);
                break;
            }

            if (alive) {
                Thread.sleep(100);
                continue;
            }

            if (killStreams.get()) {
                killStreams(thread, readCount, true);
            }

            var ex = exception.get();
            if (ex != null) {
                throw ex;
            } else {
                break;
            }
        }
    }

    private boolean checkTransferValidity() {
        var sourceFs = files.getFirst().getFileSystem();
        var targetFs = target.getFileSystem();
        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        if (!same) {
            return !sourceFs.requiresReinit() && !targetFs.requiresReinit();
        } else {
            return true;
        }
    }

    private void killStreams(Thread thread, AtomicLong transferred, boolean instant) throws Exception {
        var sourceFs = files.getFirst().getFileSystem();
        var targetFs = target.getFileSystem();
        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());

        if (!instant && !same && checkTransferValidity()) {
            var initialTransferred = transferred.get();
            if (!thread.join(Duration.ofMillis(2000))) {
                var nowTransferred = transferred.get();
                var stuck = initialTransferred == nowTransferred;
                if (stuck) {
                    sourceFs.kill();
                    targetFs.kill();
                    return;
                }
            }
        }

        if (!same) {
            if (sourceFs.getShell().isPresent()) {
                try {
                    sourceFs.getShell().get().closeStdout();
                } catch (Exception ignored) {
                }
            }

            if (targetFs.getShell().isPresent()) {
                try {
                    targetFs.getShell().get().closeStdin();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
