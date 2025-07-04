package io.xpipe.app.ext;

import io.xpipe.core.FileInfo;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.time.Instant;
import java.util.OptionalLong;

@Value
@NonFinal
public class FileEntry {
    FileSystem fileSystem;
    Instant date;

    @NonFinal
    @Setter
    String size;

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
            String size,
            FileInfo info,
            @NonNull FileKind kind) {
        this.fileSystem = fileSystem;
        this.kind = kind;
        this.path = kind == FileKind.DIRECTORY ? FilePath.of(path.toDirectory().toString()) : path;
        this.date = date;
        this.info = info;
        this.size = size;
    }

    public static FileEntry ofDirectory(FileSystem fileSystem, FilePath path) {
        return new FileEntry(fileSystem, path, Instant.now(), null, null, FileKind.DIRECTORY);
    }

    public OptionalLong getFileSizeLong() {
        if (size == null) {
            return OptionalLong.empty();
        }

        try {
            var l = Long.parseLong(size);
            return OptionalLong.of(l);
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    public FileEntry resolved() {
        return this;
    }

    public String getName() {
        return path.getFileName();
    }
}
