package io.xpipe.core.store;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;

@Value
@EqualsAndHashCode(callSuper = true)
public class LinkFileEntry extends FileEntry {

    @NonNull
    FileEntry target;

    public LinkFileEntry(
            FileSystem fileSystem, @NonNull String path, Instant date, long size, @NonNull FileInfo info,
            @NonNull FileEntry target
    ) {
        super(fileSystem, path, date, size, info,  FileKind.LINK);
        this.target = target;
    }

    public FileEntry resolved() {
        return target;
    }
}
