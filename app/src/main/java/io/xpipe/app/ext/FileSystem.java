package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public interface FileSystem extends Closeable, AutoCloseable {

    boolean writeInstantIfPossible(FileSystem sourceFs, FilePath sourceFile, FilePath targetFile) throws Exception;

    boolean readInstantIfPossible(FilePath sourceFile, FileSystem targetFs, FilePath targetFile) throws Exception;

    String getSuffix();

    boolean isRunning();

    boolean supportsLinkCreation();

    boolean supportsOwnerColumn();

    boolean supportsModeColumn();

    boolean supportsDirectorySizes();

    boolean supportsChmod();

    boolean supportsChown();

    boolean supportsChgrp();

    boolean supportsTerminalWorkingDirectory();

    Optional<ShellControl> getRawShellControl();

    void chmod(FilePath path, String mode, boolean recursive) throws Exception;

    void chown(FilePath path, String uid, boolean recursive) throws Exception;

    void chgrp(FilePath path, String gid, boolean recursive) throws Exception;

    void kill();

    void cd(FilePath dir) throws Exception;

    boolean requiresReinit();

    void reinitIfNeeded() throws Exception;

    String getFileSeparator();

    FilePath makeFileSystemCompatible(FilePath filePath);

    Optional<FilePath> pwd() throws Exception;

    FileSystem createTransferOptimizedFileSystem() throws Exception;

    long getFileSize(FilePath file) throws Exception;

    long getDirectorySize(FilePath file) throws Exception;

    Optional<ShellControl> getShell();

    FileSystem open() throws Exception;

    InputStream openInput(FilePath file) throws Exception;

    OutputStream openOutput(FilePath file, long totalBytes) throws Exception;

    boolean fileExists(FilePath file) throws Exception;

    void delete(FilePath file) throws Exception;

    void copy(FilePath file, FilePath newFile) throws Exception;

    void move(FilePath file, FilePath newFile) throws Exception;

    void mkdirs(FilePath file) throws Exception;

    void touch(FilePath file) throws Exception;

    void symbolicLink(FilePath linkFile, FilePath targetFile) throws Exception;

    boolean directoryExists(FilePath file) throws Exception;

    void directoryAccessible(FilePath file) throws Exception;

    Optional<FileEntry> getFileInfo(FilePath file) throws Exception;

    Stream<FileEntry> listFiles(FileSystem system, FilePath file) throws Exception;

    default List<FileEntry> listFilesRecursively(FileSystem system, FilePath file) throws Exception {
        var all = new ArrayList<FileEntry>();
        traverseFilesRecursively(system, file, all::add);
        return all;
    }

    default void traverseFilesRecursively(FileSystem system, FilePath file, Predicate<FileEntry> visitor)
            throws Exception {
        List<FileEntry> base;
        try (var filesStream = listFiles(system, file)) {
            base = filesStream.toList();
        }

        if (base.size() != 1) {
            for (FileEntry fileEntry : base) {
                // False will cancel the whole traversal
                if (!visitor.test(fileEntry)) {
                    return;
                }

                if (fileEntry.getKind() != FileKind.DIRECTORY) {
                    continue;
                }

                traverseFilesRecursively(system, fileEntry.getPath(), visitor);
            }
            return;
        }

        // Optimize this call to use tail recursion if possible

        if (!visitor.test(base.getFirst()) || base.getFirst().getKind() != FileKind.DIRECTORY) {
            return;
        }

        traverseFilesRecursively(system, base.getFirst().getPath(), visitor);
    }

    List<FilePath> listRoots() throws Exception;

    List<FilePath> listCommonDirectories() throws Exception;
}
