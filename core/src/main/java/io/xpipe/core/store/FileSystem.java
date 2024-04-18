package io.xpipe.core.store;

import io.xpipe.core.process.ShellControl;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface FileSystem extends Closeable, AutoCloseable {

    long getFileSize(String file) throws Exception;

    FileSystemStore getStore();

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
                        list.addAll(listFilesRecursively(fileEntry.getPath()));
                        return list.stream();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    List<String> listRoots() throws Exception;

    @Value
    @NonFinal
    class FileEntry {
        FileSystem fileSystem;
        Instant date;
        boolean hidden;
        Boolean executable;
        long size;
        String mode;

        @NonNull
        FileKind kind;

        @NonNull
        @NonFinal
        String path;

        @NonFinal
        String extension;

        @NonFinal
        String name;

        public FileEntry(
                FileSystem fileSystem,
                @NonNull String path,
                Instant date,
                boolean hidden,
                Boolean executable,
                long size,
                String mode,
                @NonNull FileKind kind) {
            this.fileSystem = fileSystem;
            this.mode = mode;
            this.kind = kind;
            this.path = kind == FileKind.DIRECTORY ? FileNames.toDirectory(path) : path;
            this.extension = FileNames.getExtension(path);
            this.name = FileNames.getFileName(path);
            this.date = date;
            this.hidden = hidden;
            this.executable = executable;
            this.size = size;
        }

        public static FileEntry ofDirectory(FileSystem fileSystem, String path) {
            return new FileEntry(fileSystem, path, Instant.now(), true, false, 0, null, FileKind.DIRECTORY);
        }

        public void setPath(String path) {
            this.path = path;
            this.extension = FileNames.getExtension(path);
            this.name = FileNames.getFileName(path);
        }

        public FileEntry resolved() {
            return this;
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    class LinkFileEntry extends FileEntry {

        @NonNull
        FileEntry target;

        public LinkFileEntry(
                @NonNull FileSystem fileSystem,
                @NonNull String path,
                Instant date,
                boolean hidden,
                Boolean executable,
                long size,
                String mode,
                @NonNull FileEntry target) {
            super(fileSystem, path, date, hidden, executable, size, mode, FileKind.LINK);
            this.target = target;
        }

        public FileEntry resolved() {
            return target;
        }
    }
}
