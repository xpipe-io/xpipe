package io.xpipe.app.browser.file;

import io.xpipe.app.browser.BrowserTransferProgress;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrowserFileTransferOperation {

    private final FileEntry target;
    private final List<FileEntry> files;
    private final BrowserFileTransferMode transferMode;
    private final boolean checkConflicts;
    private final Consumer<BrowserTransferProgress> progress;

    BrowserAlerts.FileConflictChoice lastConflictChoice;

    public BrowserFileTransferOperation(
            FileEntry target,
            List<FileEntry> files,
            BrowserFileTransferMode transferMode,
            boolean checkConflicts,
            Consumer<BrowserTransferProgress> progress) {
        this.target = target;
        this.files = files;
        this.transferMode = transferMode;
        this.checkConflicts = checkConflicts;
        this.progress = progress;
    }

    public static BrowserFileTransferOperation ofLocal(
            FileEntry target,
            List<Path> files,
            BrowserFileTransferMode transferMode,
            boolean checkConflicts,
            Consumer<BrowserTransferProgress> progress) {
        var entries = files.stream()
                .map(path -> {
                    if (!Files.exists(path)) {
                        return null;
                    }

                    try {
                        return LocalFileSystem.getLocalFileEntry(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(entry -> entry != null)
                .toList();
        return new BrowserFileTransferOperation(target, entries, transferMode, checkConflicts, progress);
    }

    private void updateProgress(BrowserTransferProgress progress) {
        this.progress.accept(progress);
    }

    private BrowserAlerts.FileConflictChoice handleChoice(FileSystem fileSystem, String target, boolean multiple) throws Exception {
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

    public void execute() throws Exception {
        if (files.isEmpty()) {
            updateProgress(null);
            return;
        }

        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        var doesMove = transferMode == BrowserFileTransferMode.MOVE
                || (same && transferMode == BrowserFileTransferMode.NORMAL);
        if (doesMove) {
            if (!BrowserAlerts.showMoveAlert(files, target)) {
                return;
            }
        }

        try {
            for (var file : files) {
                if (same) {
                    handleSingleOnSameFileSystem(file);
                } else {
                    handleSingleAcrossFileSystems(file);
                }
            }

            if (!same && doesMove) {
                for (var file : files) {
                    deleteSingle(file);
                }
            }
        } finally {
            updateProgress(null);
        }
    }

    private void handleSingleOnSameFileSystem(FileEntry source) throws Exception {
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

        if (checkConflicts) {
            var fileConflictChoice = handleChoice(target.getFileSystem(), targetFile, files.size() > 1);
            if (fileConflictChoice == BrowserAlerts.FileConflictChoice.SKIP || fileConflictChoice == BrowserAlerts.FileConflictChoice.CANCEL) {
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

    private String renameFileLoop(FileSystem fileSystem, String target, boolean dir) throws Exception {
        // Who has more than 10 copies?
        for (int i = 0; i < 10; i++) {
            target = renameFile(target);
            if ((dir && !fileSystem.directoryExists(target)) || (!dir && !fileSystem.fileExists(target))) {
                return target;
            }
        }
        return target;
    }

    private String renameFile(String target) {
        var targetFile = new FilePath(target);
        var name = targetFile.getFileName();
        var pattern = Pattern.compile("(.+?) \\((\\d+)\\)\\.(.+)");
        var matcher = pattern.matcher(name);
        if (matcher.matches()) {
            try {
                var number = Integer.parseInt(matcher.group(2));
                var newFile = targetFile.getParent().join(matcher.group(1) + " (" + (number + 1) + ")." + matcher.group(3));
                return newFile.toString();
            } catch (NumberFormatException e) {}
        }

        return targetFile.getBaseName() + " (" + 1 + ")." + targetFile.getExtension();
    }

    private void handleSingleAcrossFileSystems(FileEntry source) throws Exception {
        if (target.getKind() != FileKind.DIRECTORY) {
            throw new IllegalStateException("Target " + target.getPath() + " is not a directory");
        }

        var flatFiles = new LinkedHashMap<FileEntry, String>();

        // Prevent dropping directory into itself
        if (source.getFileSystem().equals(target.getFileSystem())
                && FileNames.startsWith(source.getPath(), target.getPath())) {
            return;
        }

        AtomicLong totalSize = new AtomicLong();
        if (source.getKind() == FileKind.DIRECTORY) {
            // Source might have been deleted meanwhile
            var exists = source.getFileSystem().directoryExists(source.getPath());
            if (!exists) {
                return;
            }

            var directoryName = FileNames.getFileName(source.getPath());
            flatFiles.put(source, directoryName);

            var baseRelative = FileNames.toDirectory(FileNames.getParent(source.getPath()));
            List<FileEntry> list = source.getFileSystem().listFilesRecursively(source.getPath());
            for (FileEntry fileEntry : list) {
                var rel = FileNames.toUnix(FileNames.relativize(baseRelative, fileEntry.getPath()));
                flatFiles.put(fileEntry, rel);
                if (fileEntry.getKind() == FileKind.FILE) {
                    // This one is up-to-date and does not need to be recalculated
                    totalSize.addAndGet(fileEntry.getSize());
                }
            }
        } else {
            // Source might have been deleted meanwhile
            var exists = source.getFileSystem().fileExists(source.getPath());
            if (!exists) {
                return;
            }

            flatFiles.put(source, FileNames.getFileName(source.getPath()));
            // Recalculate as it could have been changed meanwhile
            totalSize.addAndGet(source.getFileSystem().getFileSize(source.getPath()));
        }

        var start = Instant.now();
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
                if (checkConflicts) {
                    var fileConflictChoice = handleChoice(target.getFileSystem(), targetFile, files.size() > 1 || flatFiles.size() > 1);
                    if (fileConflictChoice == BrowserAlerts.FileConflictChoice.SKIP || fileConflictChoice == BrowserAlerts.FileConflictChoice.CANCEL) {
                        continue;
                    }

                    if (fileConflictChoice == BrowserAlerts.FileConflictChoice.RENAME) {
                        targetFile = renameFileLoop(target.getFileSystem(), targetFile, false);
                    }
                }

                transfer(sourceFile, targetFile, transferred, totalSize, start);
            }
        }
        updateProgress(BrowserTransferProgress.finished(source.getName(), totalSize.get()));
    }

    private void transfer(
            FileEntry sourceFile, String targetFile, AtomicLong transferred, AtomicLong totalSize, Instant start)
            throws Exception {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            var fileSize = sourceFile.getFileSystem().getFileSize(sourceFile.getPath());

            // Read the first few bytes to figure out possible command failure early
            // before creating the output stream
            inputStream = new BufferedInputStream(sourceFile.getFileSystem().openInput(sourceFile.getPath()), 1024);
            inputStream.mark(1024);
            var streamStart = new byte[1024];
            var streamStartLength = inputStream.read(streamStart, 0, 1024);
            if (streamStartLength < 1024) {
                inputStream.close();
                inputStream = new ByteArrayInputStream(streamStart, 0, streamStartLength);
            } else {
                inputStream.reset();
            }

            outputStream = target.getFileSystem().openOutput(targetFile, fileSize);
            transferFile(sourceFile, inputStream, outputStream, transferred, totalSize, start);
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

    private void deleteSingle(FileEntry source) throws Exception {
        source.getFileSystem().delete(source.getPath());
    }

    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private void transferFile(
            FileEntry sourceFile,
            InputStream inputStream,
            OutputStream outputStream,
            AtomicLong transferred,
            AtomicLong total,
            Instant start)
            throws IOException {
        // Initialize progress immediately prior to reading anything
        updateProgress(new BrowserTransferProgress(sourceFile.getName(), transferred.get(), total.get(), start));

        var bs = (int) Math.min(DEFAULT_BUFFER_SIZE, sourceFile.getSize());
        byte[] buffer = new byte[bs];
        int read;
        while ((read = inputStream.read(buffer, 0, bs)) > 0) {
            outputStream.write(buffer, 0, read);
            transferred.addAndGet(read);
            updateProgress(new BrowserTransferProgress(sourceFile.getName(), transferred.get(), total.get(), start));
        }
    }
}
