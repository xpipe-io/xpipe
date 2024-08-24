package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface FileSystem extends Closeable, AutoCloseable {

    long getFileSize(String file) throws Exception;

    Optional<ShellControl> getShell();

    FileSystem open() throws Exception;

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file, long totalBytes) throws Exception;

    boolean fileExists(String file) throws Exception;

    void delete(String file) throws Exception;

    void copy(String file, String newFile) throws Exception;

    void move(String file, String newFile) throws Exception;

    void mkdirs(String file) throws Exception;

    void touch(String file) throws Exception;

    void symbolicLink(String linkFile, String targetFile) throws Exception;

    boolean directoryExists(String file) throws Exception;

    void directoryAccessible(String file) throws Exception;

    Stream<FileEntry> listFiles(String file) throws Exception;

    default List<FileEntry> listFilesRecursively(String file) throws Exception {
        List<FileEntry> base;
        try (var filesStream = listFiles(file)) {
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
                        list.addAll(listFilesRecursively(fileEntry.getPath().toString()));
                        return list.stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    List<String> listRoots() throws Exception;
}
