package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.ShellStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class FileSystemHelper {

    private static OpenFileSystemModel local;

    public static String normalizeDirectoryPath(OpenFileSystemModel model, String path) {
        if (path == null) {
            return null;
        }

        path = path.trim();
        if (path.isBlank()) {
            return null;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return path;
        }

        if (shell.get().getOsType().equals(OsType.WINDOWS) && path.length() == 2 && path.endsWith(":")) {
            return path + "\\";
        }

        return FileNames.toDirectory(path);
    }

    public static OpenFileSystemModel getLocal() throws Exception {
        if (local == null) {
            var model = new OpenFileSystemModel();
            model.switchFileSystem(ShellStore.local());
            local = model;
        }

        return local;
    }

    public static FileSystem.FileEntry getLocal(Path file) throws Exception {
        return new FileSystem.FileEntry(
                getLocal().getFileSystem(),
                file.toString(),
                Files.getLastModifiedTime(file).toInstant(),
                Files.isDirectory(file),
                Files.isHidden(file),
                Files.isExecutable(file),
                Files.size(file));
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

    public static void dropFilesInto(
            FileSystem.FileEntry target, List<FileSystem.FileEntry> files, boolean explicitCopy) {
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
        FileSystem.FileEntry target, FileSystem.FileEntry file, boolean explicitCopy) {
        // Prevent dropping directory into itself
        if (FileNames.startsWith(file.getPath(), target.getPath())) {
            return;
        }

        try {
            var sourceFile = file.getPath();
            var targetFile = FileNames.join(target.getPath(), FileNames.getFileName(sourceFile));
            if (explicitCopy) {
                target.getFileSystem().copy(sourceFile, targetFile);
            } else {
                target.getFileSystem().move(sourceFile, targetFile);
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    private static void dropFileAcrossFileSystems(FileSystem.FileEntry target, FileSystem.FileEntry file) {
        var flatFiles = new HashMap<FileSystem.FileEntry, String>();

        // Prevent dropping directory into itself
        if (file.getFileSystem().equals(target.getFileSystem())
                && FileNames.startsWith(file.getPath(), target.getPath())) {
            return;
        }

        try {
            if (file.isDirectory()) {
                flatFiles.put(file, FileNames.getFileName(file.getPath()));
                try (var stream = file.getFileSystem().listFilesRecursively(file.getPath())) {
                    stream.forEach(fileEntry -> {
                        flatFiles.put(fileEntry, FileNames.relativize(file.getPath(), fileEntry.getPath()));
                    });
                }
            } else {
                flatFiles.put(file, FileNames.getFileName(file.getPath()));
            }
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
            return;
        }

        for (var e : flatFiles.entrySet()) {
            var sourceFile = e.getKey();
            var targetFile = FileNames.join(target.getPath(), e.getValue());
            try {
                if (sourceFile.getFileSystem().equals(target.getFileSystem())) {
                    throw new IllegalStateException();
                }

                if (sourceFile.isDirectory()) {
                    target.getFileSystem().mkdirs(targetFile);
                } else {
                    try (var in = sourceFile.getFileSystem().openInput(sourceFile.getPath());
                            var out = target.getFileSystem().openOutput(targetFile)) {
                        in.transferTo(out);
                    }
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
            }
        }
    }
}
