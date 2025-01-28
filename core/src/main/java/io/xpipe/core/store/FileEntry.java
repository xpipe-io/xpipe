package io.xpipe.core.store;

import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.time.Instant;

@Value
@NonFinal
public class FileEntry {
    FileSystem fileSystem;
    Instant date;
    long size;

    FileInfo info;

    @NonNull
    FileKind kind;

    @NonNull
    @NonFinal
    @Setter
    FilePath path;

    public FileEntry(
            FileSystem fileSystem,
            @NonNull FilePath path,
            Instant date,
            long size,
            FileInfo info,
            @NonNull FileKind kind) {
        this.fileSystem = fileSystem;
        this.kind = kind;
        this.path = kind == FileKind.DIRECTORY ? new FilePath(path.toDirectory().toString()) : path;
        this.date = date;
        this.info = info;
        this.size = size;
    }

    public static FileEntry ofDirectory(FileSystem fileSystem, FilePath path) {
        return new FileEntry(fileSystem, path, Instant.now(), 0, null, FileKind.DIRECTORY);
    }

    public FileEntry resolved() {
        return this;
    }

    public String getName() {
        return path.getFileName();
    }
}
