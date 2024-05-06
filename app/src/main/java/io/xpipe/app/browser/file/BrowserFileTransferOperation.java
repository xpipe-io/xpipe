package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserTransferProgress;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.store.FileSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class BrowserFileTransferOperation {

    private final FileSystem.FileEntry target;
    private final List<FileSystem.FileEntry> files;
    private final BrowserFileTransferMode transferMode;
    private final boolean checkConflicts;
    private final Consumer<BrowserTransferProgress> progress;

    BrowserAlerts.FileConflictChoice lastConflictChoice;

    public BrowserFileTransferOperation(FileSystem.FileEntry target, List<FileSystem.FileEntry> files, BrowserFileTransferMode transferMode, boolean checkConflicts,
                                        Consumer<BrowserTransferProgress> progress
    ) {
        this.target = target;
        this.files = files;
        this.transferMode = transferMode;
        this.checkConflicts = checkConflicts;
        this.progress = progress;
    }

    public static BrowserFileTransferOperation ofLocal(FileSystem.FileEntry target, List<Path> files,  BrowserFileTransferMode transferMode, boolean checkConflicts, Consumer<BrowserTransferProgress> progress) {
        var entries = files.stream()
                .map(path -> {
                    try {
                        return LocalFileSystem.getLocalFileEntry(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        return new BrowserFileTransferOperation(target, entries, transferMode, checkConflicts, progress);
    }

    private void updateProgress(BrowserTransferProgress progress) {
        this.progress.accept(progress);
    }

    private boolean handleChoice(
            FileSystem fileSystem,
            String target,
            boolean multiple)
            throws Exception {
        if (lastConflictChoice == BrowserAlerts.FileConflictChoice.CANCEL) {
            return false;
        }

        if (lastConflictChoice == BrowserAlerts.FileConflictChoice.REPLACE_ALL) {
            return true;
        }

        if (fileSystem.fileExists(target)) {
            if (lastConflictChoice == BrowserAlerts.FileConflictChoice.SKIP_ALL) {
                return false;
            }

            var choice = BrowserAlerts.showFileConflictAlert(target, multiple);
            if (choice == BrowserAlerts.FileConflictChoice.CANCEL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.CANCEL;
                return false;
            }

            if (choice == BrowserAlerts.FileConflictChoice.SKIP) {
                return false;
            }

            if (choice == BrowserAlerts.FileConflictChoice.SKIP_ALL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.SKIP_ALL;
                return false;
            }

            if (choice == BrowserAlerts.FileConflictChoice.REPLACE_ALL) {
                lastConflictChoice = BrowserAlerts.FileConflictChoice.REPLACE_ALL;
                return true;
            }
        }
        return true;
    }

    public void execute()
            throws Exception {
        if (files.isEmpty()) {
            updateProgress(BrowserTransferProgress.empty());
            return;
        }

        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        var doesMove = transferMode == BrowserFileTransferMode.MOVE || (same && transferMode == BrowserFileTransferMode.NORMAL);
        if (doesMove) {
            if (!BrowserAlerts.showMoveAlert(files, target)) {
                return;
            }
        }

        for (var file : files) {
            if (file.getFileSystem().equals(target.getFileSystem())) {
                handleSingleOnSameFileSystem(file);
                updateProgress(BrowserTransferProgress.finished(file.getName(), file.getSize()));
            } else {
                handleSingleAcrossFileSystems(file);
            }
        }
    }

    private void handleSingleOnSameFileSystem(FileSystem.FileEntry source)
            throws Exception {
        // Prevent dropping directory into itself
        if (source.getPath().equals(target.getPath())) {
            return;
        }

        var sourceFile = source.getPath();
        var targetFile = FileNames.join(target.getPath(), FileNames.getFileName(sourceFile));

        if (sourceFile.equals(targetFile)) {
            return;
        }

        if (source.getKind() == FileKind.DIRECTORY && target.getFileSystem().directoryExists(targetFile)) {
            throw ErrorEvent.expected(
                    new IllegalArgumentException("Target directory " + targetFile + " does already exist"));
        }

        if (checkConflicts && !handleChoice(target.getFileSystem(), targetFile, files.size() > 1)) {
            return;
        }

        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        var doesMove = transferMode == BrowserFileTransferMode.MOVE || (same && transferMode == BrowserFileTransferMode.NORMAL);
        if (doesMove) {
            target.getFileSystem().move(sourceFile, targetFile);
        } else {
            target.getFileSystem().copy(sourceFile, targetFile);
        }
    }

    private void handleSingleAcrossFileSystems(FileSystem.FileEntry source)
            throws Exception {
        if (target.getKind() != FileKind.DIRECTORY) {
            throw new IllegalStateException("Target " + target.getPath() + " is not a directory");
        }

        var flatFiles = new LinkedHashMap<FileSystem.FileEntry, String>();

        // Prevent dropping directory into itself
        if (source.getFileSystem().equals(target.getFileSystem())
                && FileNames.startsWith(source.getPath(), target.getPath())) {
            return;
        }

        AtomicLong totalSize = new AtomicLong();
        if (source.getKind() == FileKind.DIRECTORY) {
            var directoryName = FileNames.getFileName(source.getPath());
            flatFiles.put(source, directoryName);

            var baseRelative = FileNames.toDirectory(FileNames.getParent(source.getPath()));
            List<FileSystem.FileEntry> list = source.getFileSystem().listFilesRecursively(source.getPath());
            for (FileSystem.FileEntry fileEntry : list) {
                var rel = FileNames.toUnix(FileNames.relativize(baseRelative, fileEntry.getPath()));
                flatFiles.put(fileEntry, rel);
                if (fileEntry.getKind() == FileKind.FILE) {
                    // This one is up-to-date and does not need to be recalculated
                    totalSize.addAndGet(fileEntry.getSize());
                }
            }
        } else {
            flatFiles.put(source, FileNames.getFileName(source.getPath()));
            // Recalculate as it could have been changed meanwhile
            totalSize.addAndGet(source.getFileSystem().getFileSize(source.getPath()));
        }

        AtomicLong transferred = new AtomicLong();
        for (var e : flatFiles.entrySet()) {
            var sourceFile = e.getKey();
            var fixedRelPath = new FilePath(e.getValue())
                    .fileSystemCompatible(
                            target.getFileSystem().getShell().orElseThrow().getOsType());
            var targetFile = FileNames.join(target.getPath(), fixedRelPath.toString());
            if (sourceFile.getFileSystem().equals(target.getFileSystem())) {
                throw new IllegalStateException();
            }

            if (sourceFile.getKind() == FileKind.DIRECTORY) {
                target.getFileSystem().mkdirs(targetFile);
            } else if (sourceFile.getKind() == FileKind.FILE) {
                if (checkConflicts
                        && !handleChoice(
                        target.getFileSystem(),
                        targetFile,
                        files.size() > 1 || flatFiles.size() > 1)) {
                    continue;
                }

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    var fileSize = sourceFile.getFileSystem().getFileSize(sourceFile.getPath());
                    inputStream = sourceFile.getFileSystem().openInput(sourceFile.getPath());
                    outputStream = target.getFileSystem().openOutput(targetFile, fileSize);
                    transferFile(sourceFile, inputStream, outputStream, transferred, totalSize);
                    inputStream.transferTo(OutputStream.nullOutputStream());
                } catch (Exception ex) {
                    // Mark progress as finished to reset any progress display
                    updateProgress(BrowserTransferProgress.finished(sourceFile.getName(), transferred.get()));

                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception om) {
                            // This is expected as the process control has to be killed
                            // When calling close, it will throw an exception when it has to kill
                            // ErrorEvent.fromThrowable(om).handle();
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Exception om) {
                            // This is expected as the process control has to be killed
                            // When calling close, it will throw an exception when it has to kill
                            // ErrorEvent.fromThrowable(om).handle();
                        }
                    }
                    throw ex;
                }

                updateProgress(BrowserTransferProgress.finished(sourceFile.getName(), transferred.get()));
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
                        ErrorEvent.fromThrowable(om).handle();
                    } else {
                        exception = om;
                    }
                }
                if (exception != null) {
                    throw exception;
                }
            }
        }
        updateProgress(BrowserTransferProgress.finished(source.getName(), totalSize.get()));
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private void transferFile(
            FileSystem.FileEntry sourceFile,
            InputStream inputStream,
            OutputStream outputStream,
            AtomicLong transferred,
            AtomicLong total)
            throws IOException {
        // Initialize progress immediately prior to reading anything
        var now = Instant.now();
        updateProgress(new BrowserTransferProgress(sourceFile.getName(), transferred.get(), total.get(), now));

        var bs = (int) Math.min(DEFAULT_BUFFER_SIZE, sourceFile.getSize());
        byte[] buffer = new byte[bs];
        int read;
        while ((read = inputStream.read(buffer, 0, bs)) > 0) {
            outputStream.write(buffer, 0, read);
            transferred.addAndGet(read);
            updateProgress(new BrowserTransferProgress(sourceFile.getName(), transferred.get(), total.get(), now));
        }
    }
}
