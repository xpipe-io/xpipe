package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface FileSystem extends Closeable, AutoCloseable {

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
        List<FileEntry> base;
        try (var filesStream = listFiles(system, file)) {
            base = filesStream.toList();
        }
        return base.stream()
                .flatMap(fileEntry -> {
                    if (fileEntry.getKind() != FileKind.DIRECTORY) {
                        return Stream.of(fileEntry);
                    }

                    try {
                        var list = new ArrayList<FileEntry>();
                        list.add(fileEntry);
                        list.addAll(listFilesRecursively(system, fileEntry.getPath()));
                        return list.stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    List<FilePath> listRoots() throws Exception;
}
