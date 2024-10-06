package io.xpipe.app.browser.file;

import io.xpipe.app.browser.fs.OpenFileSystemModel;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileKind;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.store.FileSystem;

import java.time.Instant;
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
            ErrorEvent.expected(ex);
            throw ex;
        }
    }

    public static String resolveDirectoryPath(OpenFileSystemModel model, String path, boolean allowRewrite)
            throws Exception {
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
                .readStdoutOrThrow();

        if (!FileNames.isAbsolute(resolved)) {
            throw new IllegalArgumentException(String.format("Directory %s is not absolute", resolved));
        }

        if (allowRewrite && model.getFileSystem().fileExists(path)) {
            return FileNames.toDirectory(FileNames.getParent(path));
        }

        return FileNames.toDirectory(resolved);
    }

    public static void validateDirectoryPath(OpenFileSystemModel model, String path, boolean verifyExists)
            throws Exception {
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

        if (verifyExists && !model.getFileSystem().directoryExists(path)) {
            throw ErrorEvent.expected(new IllegalArgumentException(String.format("Directory %s does not exist", path)));
        }

        try {
            model.getFileSystem().directoryAccessible(path);
        } catch (Exception ex) {
            ErrorEvent.expected(ex);
            throw ex;
        }
    }

    public static FileEntry getRemoteWrapper(FileSystem fileSystem, String file) throws Exception {
        return new FileEntry(
                fileSystem,
                file,
                Instant.now(),
                fileSystem.getFileSize(file),
                null,
                fileSystem.directoryExists(file) ? FileKind.DIRECTORY : FileKind.FILE);
    }

    public static void delete(List<FileEntry> files) {
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
}
