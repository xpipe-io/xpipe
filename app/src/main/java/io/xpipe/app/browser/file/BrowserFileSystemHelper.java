package io.xpipe.app.browser.file;

import io.xpipe.app.ext.FileEntry;
import io.xpipe.app.ext.FileSystem;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import java.time.Instant;
import java.util.List;

public class BrowserFileSystemHelper {

    public static String adjustPath(BrowserFileSystemTabModel model, String path) {
        if (path == null) {
            return null;
        }

        path = path.strip();
        if (path.isBlank()) {
            return null;
        }

        if (path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1);
        } else if (path.startsWith("'") && path.endsWith("'")) {
            path = path.substring(1, path.length() - 1);
        }

        // Handle special case when file system creation has failed

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return path;
        }

        if (shell.get().getOsType() == OsType.WINDOWS && path.length() == 2 && path.endsWith(":")) {
            return path + "\\";
        }

        return path;
    }

    public static String evaluatePath(BrowserFileSystemTabModel model, String path) throws Exception {
        if (path == null) {
            return null;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty() || !shell.get().isRunning(true)) {
            return path;
        }

        try {
            var r = shell.get()
                    .getShellDialect()
                    .evaluateExpression(shell.get(), path)
                    .readStdoutOrThrow();
            return !r.isBlank() ? r : null;
        } catch (Exception ex) {
            ErrorEventFactory.expected(ex);
            throw ex;
        }
    }

    public static FilePath resolveDirectoryPath(BrowserFileSystemTabModel model, FilePath path, boolean allowRewrite)
            throws Exception {
        if (path == null) {
            return null;
        }

        var shell = model.getFileSystem().getShell();
        if (shell.isEmpty()) {
            return path;
        }

        var tildeResolved = path.resolveTildeHome(
                model.getFileSystem().getShell().orElseThrow().view().userHome());
        var resolved = FilePath.of(shell.get()
                .getShellDialect()
                .resolveDirectory(shell.get(), tildeResolved.toString())
                .readStdoutOrThrow());

        if (!resolved.isAbsolute()) {
            throw ErrorEventFactory.expected(
                    new IllegalArgumentException(String.format("Directory %s is not absolute", resolved)));
        }

        if (allowRewrite && model.getFileSystem().fileExists(resolved)) {
            return resolved.getParent().toDirectory();
        }

        return resolved.toDirectory();
    }

    public static void validateDirectoryPath(BrowserFileSystemTabModel model, FilePath path, boolean verifyExists)
            throws Exception {
        if (path == null) {
            return;
        }

        if (verifyExists && !model.getFileSystem().directoryExists(path)) {
            throw ErrorEventFactory.expected(new IllegalArgumentException(
                    String.format("Directory %s does not exist or is not accessible", path)));
        }

        try {
            model.getFileSystem().directoryAccessible(path);
        } catch (Exception ex) {
            ErrorEventFactory.expected(ex);
            throw ex;
        }
    }

    public static FileEntry getRemoteWrapper(FileSystem fileSystem, FilePath file) throws Exception {
        return new FileEntry(
                fileSystem,
                file,
                Instant.now(),
                "" + fileSystem.getFileSize(file),
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
                ErrorEventFactory.fromThrowable(t).handle();
            }
        }
    }
}
