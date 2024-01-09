package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.LocalStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class FileSystemHelper {

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
            ErrorEvent.unreportable(ex);
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
            throw ErrorEvent.unreportable(new IllegalArgumentException(String.format("Directory %s does not exist", path)));
        }

        try {
            model.getFileSystem().directoryAccessible(path);
        } catch (Exception ex) {
            ErrorEvent.unreportable(ex);
            throw ex;
        }
    }

    private static FileSystem localFileSystem;

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

    public static void dropLocalFilesInto(FileSystem.FileEntry entry, List<Path> files) {
        try {
            var entries = files.stream()
                    .map(path -> {
                        try {
                            return getLocal(path);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            dropFilesInto(entry, entries, false);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    public static void delete(List<FileSystem.FileEntry> files) {
        if (files.size() == 0) {
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
            FileSystem.FileEntry target, List<FileSystem.FileEntry> files, boolean explicitCopy) throws Exception {
        if (files.size() == 0) {
            return;
        }

        for (var file : files) {
            if (file.getFileSystem().equals(target.getFileSystem())) {
                dropFileAcrossSameFileSystem(target, file, explicitCopy);
            } else {
                dropFileAcrossFileSystems(target, file);
            }
        }
    }

    private static void dropFileAcrossSameFileSystem(
            FileSystem.FileEntry target, FileSystem.FileEntry source, boolean explicitCopy) throws Exception {
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
            throw ErrorEvent.unreportable(new IllegalArgumentException("Target directory " + targetFile + " does already exist"));
        }

        if (explicitCopy) {
            target.getFileSystem().copy(sourceFile, targetFile);
        } else {
            target.getFileSystem().move(sourceFile, targetFile);
        }
    }

    private static void dropFileAcrossFileSystems(FileSystem.FileEntry target, FileSystem.FileEntry source)
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

        if (source.getKind() == FileKind.DIRECTORY) {
            var directoryName = FileNames.getFileName(source.getPath());
            flatFiles.put(source, directoryName);

            var baseRelative = FileNames.toDirectory(FileNames.getParent(source.getPath()));
            List<FileSystem.FileEntry> list = source.getFileSystem().listFilesRecursively(source.getPath());
            list.forEach(fileEntry -> {
                flatFiles.put(fileEntry, FileNames.toUnix(FileNames.relativize(baseRelative, fileEntry.getPath())));
            });
        } else {
            flatFiles.put(source, FileNames.getFileName(source.getPath()));
        }

        for (var e : flatFiles.entrySet()) {
            var sourceFile = e.getKey();
            var targetFile = FileNames.join(target.getPath(), e.getValue());
            if (sourceFile.getFileSystem().equals(target.getFileSystem())) {
                throw new IllegalStateException();
            }

            if (sourceFile.getKind() == FileKind.DIRECTORY) {
                target.getFileSystem().mkdirs(targetFile);
            } else if (sourceFile.getKind() == FileKind.FILE) {
                try (var in = sourceFile.getFileSystem().openInput(sourceFile.getPath());
                        var out = target.getFileSystem().openOutput(targetFile)) {
                    in.transferTo(out);
                }
            }
        }
    }
}
