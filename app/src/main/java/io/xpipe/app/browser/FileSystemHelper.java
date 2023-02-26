package io.xpipe.app.browser;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class FileSystemHelper {

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

    public static FileSystem.FileEntry getLocal(Path file) throws Exception {
        return new FileSystem.FileEntry(
                LocalStore.getFileSystem(),
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
        if (FileNames.startsWith(source.getPath(), target.getPath())) {
            return;
        }

        var sourceFile = source.getPath();
        var targetFile = FileNames.join(target.getPath(), FileNames.getFileName(sourceFile));
        if (explicitCopy) {
            target.getFileSystem().copy(sourceFile, targetFile);
        } else {
            target.getFileSystem().move(sourceFile, targetFile);
        }
    }

    private static void dropFileAcrossFileSystems(FileSystem.FileEntry target, FileSystem.FileEntry source)
            throws Exception {
        var flatFiles = new LinkedHashMap<FileSystem.FileEntry, String>();

        // Prevent dropping directory into itself
        if (source.getFileSystem().equals(target.getFileSystem())
                && FileNames.startsWith(source.getPath(), target.getPath())) {
            return;
        }

        if (source.isDirectory()) {
            var directoryName = FileNames.getFileName(source.getPath());
            flatFiles.put(source, directoryName);

            var baseRelative = FileNames.toDirectory(FileNames.getParent(source.getPath()));
            try (var stream = source.getFileSystem().listFilesRecursively(source.getPath())) {
                stream.forEach(fileEntry -> {
                    flatFiles.put(fileEntry, FileNames.toUnix(FileNames.relativize(baseRelative, fileEntry.getPath())));
                });
            }
        } else {
            flatFiles.put(source, FileNames.getFileName(source.getPath()));
        }

        for (var e : flatFiles.entrySet()) {
            var sourceFile = e.getKey();
            var targetFile = FileNames.join(target.getPath(), e.getValue());
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
        }
    }
}
