package io.xpipe.app.ext;

import io.xpipe.core.FileInfo;
import io.xpipe.core.FileKind;
import io.xpipe.core.FilePath;
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
            FileSystem fileSystem,
            @NonNull FilePath path,
            Instant date,
            String size,
            @NonNull FileInfo info,
            @NonNull FileEntry target) {
        super(fileSystem, path, date, size, info, FileKind.LINK);
        this.target = target;
    }

    public FileEntry resolved() {
        return target;
    }
}
