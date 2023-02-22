package io.xpipe.core.store;

import io.xpipe.core.process.ShellProcessControl;
import lombok.NonNull;
import lombok.Value;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface FileSystem extends Closeable, AutoCloseable {

    @Value
    static class FileEntry {
        @NonNull
        FileSystem fileSystem;
        @NonNull
        String path;
        @NonNull
        Instant date;
        boolean directory;
        boolean hidden;
        Boolean executable;
        long size;
    }

    Optional<ShellProcessControl> getShell();

    FileSystem open() throws Exception;

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws Exception;

    public boolean exists(String file) throws Exception;

    public  void delete(String file) throws Exception;

    void copy(String file, String newFile) throws Exception;

    void move(String file, String newFile) throws Exception;

    boolean mkdirs(String file) throws Exception;

    void touch(String file) throws Exception;

    boolean isDirectory(String file) throws Exception;

    Stream<FileEntry> listFiles(String file) throws Exception;

    default Stream<FileEntry> listFilesRecursively(String file) throws Exception {
        return listFiles(file).flatMap(fileEntry -> {
            try {
                return listFilesRecursively(fileEntry.getPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    List<String> listRoots() throws Exception;
}
