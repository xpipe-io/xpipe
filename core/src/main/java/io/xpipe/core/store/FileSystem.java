package io.xpipe.core.store;

import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellControl;
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
        Instant date;
        boolean hidden;
        Boolean executable;
        long size;
        String mode;
        @NonNull
        FileKind kind;

        public FileEntry(
                @NonNull FileSystem fileSystem, @NonNull String path, Instant date, boolean hidden, Boolean executable,
                long size,
                String mode,
                @NonNull FileKind kind
        ) {
            this.fileSystem = fileSystem;
            this.mode = mode;
            this.kind = kind;
            this.path = kind == FileKind.DIRECTORY ? FileNames.toDirectory(path) : path;
            this.date = date;
            this.hidden = hidden;
            this.executable = executable;
            this.size = size;
        }

        public static FileEntry ofDirectory(FileSystem fileSystem, String path) {
            return new FileEntry(fileSystem, path, Instant.now(), true, false, 0, null, FileKind.DIRECTORY);
        }

    }

    FileSystemStore getStore();

    Optional<ShellControl> getShell();

    FileSystem open() throws Exception;

    InputStream openInput(String file) throws Exception;

    OutputStream openOutput(String file) throws Exception;

    public boolean fileExists(String file) throws Exception;

    public  void delete(String file) throws Exception;

    void copy(String file, String newFile) throws Exception;

    void move(String file, String newFile) throws Exception;

    void mkdirs(String file) throws Exception;

    void touch(String file) throws Exception;

    boolean directoryExists(String file) throws Exception;

    void directoryAccessible(String file) throws Exception;

    Stream<FileEntry> listFiles(String file) throws Exception;

    default Stream<FileEntry> listFilesRecursively(String file) throws Exception {
        return listFiles(file).flatMap(fileEntry -> {
            if (fileEntry.getKind() != FileKind.DIRECTORY) {
                return Stream.of(fileEntry);
            }

            try {
                return Stream.concat(Stream.of(fileEntry), listFilesRecursively(fileEntry.getPath()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    List<String> listRoots() throws Exception;
}
