package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.LocalStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FileSystemHelper {

    private static final int DEFAULT_BUFFER_SIZE = 16384;
    private static FileSystem localFileSystem;

    public static String adjustPath(OpenFileSystemModel model, String path) {
        if (path == null) {
            return null;
        }

        path = path.trim();
        if (path.isBlank()) {
            return null;
        }

        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        } else if (path.startsWith("'") && path.endsWith("'")) {
            path = path.substring(1, path.length() - 1);
        }

        // Handle special case when file system creation has failed
        if (model.getFileSystem() == null) {
            return path;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return path;
        }

        if (shell.get().getOsType().equals(OsType.WINDOWS) && path.length() == 2 && path.endsWith(":")) {
            return path + "\\";
        }

        return path;
    }

    public static String evaluatePath(OpenFileSystemModel model, String path) throws Exception {
        if (path == null) {
            return null;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty() || !shell.get().isRunning()) {
            return path;
        }

        try {
            return shell.get()
                    .getShellDialect()
                    .evaluateExpression(shell.get(), path)
                    .readStdoutOrThrow();
        } catch (Exception ex) {
            ErrorEvent.expected(ex);
            throw ex;
        }
    }

    public static String resolveDirectoryPath(OpenFileSystemModel model, String path) throws Exception {
        if (path == null) {
            return null;
        }

        if (model.getFileSystem() == null) {
            return path;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return path;
        }

        var resolved = shell.get()
                .getShellDialect()
                .resolveDirectory(shell.get(), path)
                .withWorkingDirectory(model.getCurrentPath().get())
                .readStdoutOrThrow();

        if (!FileNames.isAbsolute(resolved)) {
            throw new IllegalArgumentException(String.format("Directory %s is not absolute", resolved));
        }

        if (model.getFileSystem().fileExists(path)) {
            return FileNames.toDirectory(FileNames.getParent(path));
        }

        return FileNames.toDirectory(resolved);
    }

    public static void validateDirectoryPath(OpenFileSystemModel model, String path) throws Exception {
        if (path == null) {
            return;
        }

        if (model.getFileSystem() == null) {
            return;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return;
        }

        if (!model.getFileSystem().directoryExists(path)) {
            throw ErrorEvent.expected(
                    new IllegalArgumentException(String.format("Directory %s does not exist", path)));
        }

        try {
            model.getFileSystem().directoryAccessible(path);
        } catch (Exception ex) {
            ErrorEvent.expected(ex);
            throw ex;
        }
    }

    public static FileSystem.FileEntry getLocal(Path file) throws Exception {
        if (localFileSystem == null) {
            localFileSystem = new LocalStore().createFileSystem();
            localFileSystem.open();
        }

        return new FileSystem.FileEntry(
                localFileSystem,
                file.toString(),
                Files.getLastModifiedTime(file).toInstant(),
                Files.isHidden(file),
                Files.isExecutable(file),
                Files.size(file),
                null,
                Files.isDirectory(file) ? FileKind.DIRECTORY : FileKind.FILE);
    }

    public static void dropLocalFilesInto(
            FileSystem.FileEntry entry, List<Path> files, Consumer<BrowserTransferProgress> progress, boolean checkConflicts) throws Exception {
        var entries = files.stream()
                .map(path -> {
                    try {
                        return getLocal(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
        dropFilesInto(entry, entries, false, checkConflicts, progress);
    }

    public static void delete(List<FileSystem.FileEntry> files) {
        if (files.isEmpty()) {
            return;
        }

        for (var file : files) {
            try {
                file.getFileSystem().delete(file.getPath());
            } catch (Throwable t) {
                ErrorEvent.fromThrowable(t).handle();
            }
        }
    }

    public static void dropFilesInto(
            FileSystem.FileEntry target,
            List<FileSystem.FileEntry> files,
            boolean explicitCopy,
            boolean checkConflicts,
            Consumer<BrowserTransferProgress> progress)
            throws Exception {
        if (files.isEmpty()) {
            progress.accept(BrowserTransferProgress.empty());
            return;
        }

        var same = files.getFirst().getFileSystem().equals(target.getFileSystem());
        if (same && !explicitCopy) {
            if (!BrowserAlerts.showMoveAlert(files, target)) {
                return;
            }
        }

        AtomicReference<BrowserAlerts.FileConflictChoice> lastConflictChoice = new AtomicReference<>();
        for (var file : files) {
            if (file.getFileSystem().equals(target.getFileSystem())) {
                dropFileAcrossSameFileSystem(target, file, explicitCopy, lastConflictChoice, files.size() > 1, checkConflicts);
                progress.accept(BrowserTransferProgress.finished(file.getName(), file.getSize()));
            } else {
                dropFileAcrossFileSystems(target, file, progress, lastConflictChoice, files.size() > 1, checkConflicts);
            }
        }
    }

    private static void dropFileAcrossSameFileSystem(
            FileSystem.FileEntry target,
            FileSystem.FileEntry source,
            boolean explicitCopy,
            AtomicReference<BrowserAlerts.FileConflictChoice> lastConflictChoice,
            boolean multiple,
            boolean checkConflicts)
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

        if (checkConflicts && !handleChoice(lastConflictChoice, target.getFileSystem(), targetFile, multiple)) {
            return;
        }

        if (explicitCopy) {
            target.getFileSystem().copy(sourceFile, targetFile);
        } else {
            target.getFileSystem().move(sourceFile, targetFile);
        }
    }

    private static void dropFileAcrossFileSystems(
            FileSystem.FileEntry target,
            FileSystem.FileEntry source,
            Consumer<BrowserTransferProgress> progress,
            AtomicReference<BrowserAlerts.FileConflictChoice> lastConflictChoice,
            boolean multiple,
            boolean checkConflicts)
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
                flatFiles.put(fileEntry, FileNames.toUnix(FileNames.relativize(baseRelative, fileEntry.getPath())));
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
            var targetFile = FileNames.join(target.getPath(), e.getValue());
            if (sourceFile.getFileSystem().equals(target.getFileSystem())) {
                throw new IllegalStateException();
            }

            if (sourceFile.getKind() == FileKind.DIRECTORY) {
                target.getFileSystem().mkdirs(targetFile);
            } else if (sourceFile.getKind() == FileKind.FILE) {
                if (checkConflicts && !handleChoice(
                        lastConflictChoice, target.getFileSystem(), targetFile, multiple || flatFiles.size() > 1)) {
                    continue;
                }

                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    var fileSize = sourceFile.getFileSystem().getFileSize(sourceFile.getPath());
                    inputStream = sourceFile.getFileSystem().openInput(sourceFile.getPath());
                    outputStream = target.getFileSystem().openOutput(targetFile, fileSize);
                    transferFile(sourceFile, inputStream, outputStream, transferred, totalSize, progress);
                    inputStream.transferTo(OutputStream.nullOutputStream());
                } catch (Exception ex) {
                    // Mark progress as finished to reset any progress display
                    progress.accept(BrowserTransferProgress.finished(sourceFile.getName(), transferred.get()));

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

                progress.accept(BrowserTransferProgress.finished(sourceFile.getName(), transferred.get()));
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
        progress.accept(BrowserTransferProgress.finished(source.getName(), totalSize.get()));
    }

    private static boolean handleChoice(
            AtomicReference<BrowserAlerts.FileConflictChoice> previous,
            FileSystem fileSystem,
            String target,
            boolean multiple)
            throws Exception {
        if (previous.get() == BrowserAlerts.FileConflictChoice.CANCEL) {
            return false;
        }

        if (previous.get() == BrowserAlerts.FileConflictChoice.REPLACE_ALL) {
            return true;
        }

        if (fileSystem.fileExists(target)) {
            if (previous.get() == BrowserAlerts.FileConflictChoice.SKIP_ALL) {
                return false;
            }

            var choice = BrowserAlerts.showFileConflictAlert(target, multiple);
            if (choice == BrowserAlerts.FileConflictChoice.CANCEL) {
                previous.set(BrowserAlerts.FileConflictChoice.CANCEL);
                return false;
            }

            if (choice == BrowserAlerts.FileConflictChoice.SKIP) {
                return false;
            }

            if (choice == BrowserAlerts.FileConflictChoice.SKIP_ALL) {
                previous.set(BrowserAlerts.FileConflictChoice.SKIP_ALL);
                return false;
            }

            if (choice == BrowserAlerts.FileConflictChoice.REPLACE_ALL) {
                previous.set(BrowserAlerts.FileConflictChoice.REPLACE_ALL);
                return true;
            }
        }
        return true;
    }

    private static void transferFile(
            FileSystem.FileEntry sourceFile,
            InputStream inputStream,
            OutputStream outputStream,
            AtomicLong transferred,
            AtomicLong total,
            Consumer<BrowserTransferProgress> progress)
            throws IOException {
        // Initialize progress immediately prior to reading anything
        progress.accept(new BrowserTransferProgress(sourceFile.getName(), transferred.get(), total.get()));

        var bs = (int) Math.min(DEFAULT_BUFFER_SIZE, sourceFile.getSize());
        byte[] buffer = new byte[bs];
        int read;
        while ((read = inputStream.read(buffer, 0, bs)) > 0) {
            outputStream.write(buffer, 0, read);
            transferred.addAndGet(read);
            progress.accept(new BrowserTransferProgress(sourceFile.getName(), transferred.get(), total.get()));
        }
    }
}
