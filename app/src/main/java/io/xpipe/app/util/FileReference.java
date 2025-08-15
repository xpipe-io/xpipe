package io.xpipe.app.util;

import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.FilePath;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a file located on a file system.
 */
@SuperBuilder
@Jacksonized
@Value
public class FileReference {

    DataStoreEntryRef<? extends FileSystemStore> fileSystem;
    FilePath path;

    public FileReference(DataStoreEntryRef<? extends FileSystemStore> fileSystem, FilePath path) {
        this.fileSystem = fileSystem;
        this.path = path;
    }
}
