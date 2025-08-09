package io.xpipe.app.browser.file;

import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileSystem;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;

import lombok.Getter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class BrowserFileTransferOperation {

    @Getter
    private final FileEntry target;

    @Getter
    private final List<FileEntry> files;

    private final BrowserFileTransferMode transferMode;
    private final boolean checkConflicts;
    private final Consumer<BrowserTransferProgress> progress;
    private final BooleanProperty cancelled;

    BrowserAlerts.FileConflictChoice lastConflictChoice;

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

    private void updateProgress(BrowserTransferProgress progress) {
        this.progress.accept(progress);
    }

    private BrowserAlerts.FileConflictChoice handleChoice(FileSystem fileSystem, FilePath target, boolean multiple)
            throws Exception {
        if (lastConflictChoice == BrowserAlerts.FileConflictChoice.CANCEL) {
            return BrowserAlerts.FileConflictChoice.CANCEL;
        }

        if (lastConflictChoice == BrowserAlerts.FileConflictChoice.REPLACE_ALL) {
            return BrowserAlerts.FileConflictChoice.REPLACE;
        }

        if (lastConflictChoice == BrowserAlerts.FileConflictChoice.RENAME_ALL) {
            return BrowserAlerts.FileConflictChoice.RENAME;
        }

        if (fileSystem.fileExists(target)) {
            if (lastConflictChoice == BrowserAlerts.FileConflictChoice.SKIP_ALL) {
                return BrowserAlerts.FileConflictChoice.SKIP;
            }

            var choice = BrowserAlerts.showFileConflictAlert(target, multiple);
            if (choice == BrowserAlerts.FileConflictChoice.CANCEL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.CANCEL;
                return BrowserAlerts.FileConflictChoice.CANCEL;
            }

            if (choice == BrowserAlerts.FileConflictChoice.SKIP) {
                return BrowserAlerts.FileConflictChoice.SKIP;
            }

            if (choice == BrowserAlerts.FileConflictChoice.SKIP_ALL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.SKIP_ALL;
                return BrowserAlerts.FileConflictChoice.SKIP;
            }

            if (choice == BrowserAlerts.FileConflictChoice.REPLACE_ALL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.REPLACE_ALL;
                return BrowserAlerts.FileConflictChoice.REPLACE;
            }

            if (choice == BrowserAlerts.FileConflictChoice.RENAME_ALL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.RENAME_ALL;
                return BrowserAlerts.FileConflictChoice.RENAME;
            }

            return choice;
        }
        return BrowserAlerts.FileConflictChoice.REPLACE;
    }

    private boolean cancelled() {
        return cancelled.get();
    }

    public boolean isMove() {
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
                    var currentDir =
                            file.getFileSystem().getShell().orElseThrow().view().pwd();
                    handleSingleAcrossFileSystems(file);
                    file.getFileSystem().getShell().orElseThrow().view().cd(currentDir);
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
            targetFile = renameFileLoop(target.getFileSystem(), targetFile, source.getKind() == FileKind.DIRECTORY);
        }

        if (source.getKind() == FileKind.DIRECTORY && target.getFileSystem().directoryExists(targetFile)) {
            throw ErrorEventFactory.expected(
                    new IllegalArgumentException("Target directory " + targetFile + " does already exist"));
        }

        if (checkConflicts) {
            var fileConflictChoice = handleChoice(target.getFileSystem(), targetFile, files.size() > 1);
            if (fileConflictChoice == BrowserAlerts.FileConflictChoice.SKIP
                    || fileConflictChoice == BrowserAlerts.FileConflictChoice.CANCEL) {
                return;
            }

            if (fileConflictChoice == BrowserAlerts.FileConflictChoice.RENAME) {
                targetFile = renameFileLoop(target.getFileSystem(), targetFile, source.getKind() == FileKind.DIRECTORY);
            }
        }

        var doesMove = transferMode == BrowserFileTransferMode.MOVE || transferMode == BrowserFileTransferMode.NORMAL;
        if (doesMove) {
            target.getFileSystem().move(sourceFile, targetFile);
        } else {
            target.getFileSystem().copy(sourceFile, targetFile);
        }
    }

    private FilePath renameFileLoop(FileSystem fileSystem, FilePath target, boolean dir) throws Exception {
        // Who has more than 10 copies?
        for (int i = 0; i < 10; i++) {
            target = renameFile(target);
            if ((dir && !fileSystem.directoryExists(target)) || (!dir && !fileSystem.fileExists(target))) {
                return target;
            }
        }
        return target;
    }

    private FilePath renameFile(FilePath target) {
        var name = target.getFileName();
        var pattern = Pattern.compile("(.+) \\((\\d+)\\)\\.(.+?)");
        var matcher = pattern.matcher(name);
        if (matcher.matches()) {
            try {
                var number = Integer.parseInt(matcher.group(2));
                var newFile = target.getParent().join(matcher.group(1) + " (" + (number + 1) + ")." + matcher.group(3));
                return newFile;
            } catch (NumberFormatException ignored) {
            }
        }

        var ext = target.getExtension();
        return FilePath.of(target.getBaseName() + " (" + 1 + ")" + (ext.isPresent() ? "." + ext.get() : ""));
    }

    private void handleSingleAcrossFileSystems(FileEntry source) throws Exception {
        if (target.getKind() != FileKind.DIRECTORY) {
            throw new IllegalStateException("Target " + target.getPath() + " is not a directory");
        }

        var flatFiles = new LinkedHashMap<FileEntry, String>();

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
                return;
            }

            var directoryName = source.getPath().getFileName();
            flatFiles.put(source, directoryName);

            var baseRelative = source.getPath().getParent().toDirectory();
            try (var stream = source.getFileSystem().listFilesRecursively(source.getFileSystem(), source.getPath())) {
                List<FileEntry> list = stream.toList();
                for (FileEntry fileEntry : list) {
                    if (cancelled()) {
                        return;
                    }

                    var rel = fileEntry.getPath().relativize(baseRelative).toUnix().toString();
                    flatFiles.put(fileEntry, rel);
                    if (fileEntry.getKind() == FileKind.FILE) {
                        // This one is up-to-date and does not need to be recalculated
                        // If we don't have a size, it doesn't matter that much as the total size is only for display
                        totalSize.addAndGet(fileEntry.getFileSizeLong().orElse(0));
                    }
                }
            }
        } else {
            // Source might have been deleted meanwhile
            var exists = source.getFileSystem().fileExists(source.getPath());
            if (!exists) {
                return;
            }

            flatFiles.put(source, source.getPath().getFileName());
            // If we don't have a size, it doesn't matter that much as the total size is only for display
            totalSize.addAndGet(source.getFileSizeLong().orElse(0));
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
                var os = targetFs.getShell().orElseThrow().getOsType();
                var fixedRelPath = OsFileSystem.of(os).makeFileSystemCompatible(FilePath.of(e.getValue()));
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
                        if (fileConflictChoice == BrowserAlerts.FileConflictChoice.SKIP
                                || fileConflictChoice == BrowserAlerts.FileConflictChoice.CANCEL) {
                            continue;
                        }

                        if (fileConflictChoice == BrowserAlerts.FileConflictChoice.RENAME) {
                            targetFile = renameFileLoop(targetFs, targetFile, false);
                        }
                    }

                    transfer(
                            sourceFile.getPath(),
                            optimizedSourceFs,
                            targetFile,
                            targetFs,
                            transferred,
                            totalSize);
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

            sourceFs.getShell().orElseThrow().killExternal();
            targetFs.getShell().orElseThrow().killExternal();
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

    private static final int DEFAULT_BUFFER_SIZE = 1024;

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
                    updateProgress(new BrowserTransferProgress(
                            sourceFile.getFileName(), transferred.get(), total.get()));
                }

                outputStream.flush();
                inputStream.transferTo(OutputStream.nullOutputStream());

                var incomplete = readCount.get() < expectedFileSize;
                if (incomplete) {
                    throw new IOException("Source file " + sourceFile + " input did end prematurely");
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
            var sourceShell = sourceFs.getShell().orElseThrow();
            var targetShell = targetFs.getShell().orElseThrow();
            // Check for null on shell reset
            return sourceShell.getStdout() != null
                    && !sourceShell.getStdout().isClosed()
                    && targetShell.getStdin() != null
                    && !targetShell.getStdin().isClosed();
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
            if (!thread.join(Duration.ofMillis(1000))) {
                var nowTransferred = transferred.get();
                var stuck = initialTransferred == nowTransferred;
                if (stuck) {
                    sourceFs.getShell().orElseThrow().killExternal();
                    targetFs.getShell().orElseThrow().killExternal();
                    return;
                }
            }
        }

        if (!same) {
            var sourceShell = sourceFs.getShell().orElseThrow();
            var targetShell = targetFs.getShell().orElseThrow();
            try {
                sourceShell.closeStdout();
            } finally {
                targetShell.closeStdin();
            }
        }
    }
}
