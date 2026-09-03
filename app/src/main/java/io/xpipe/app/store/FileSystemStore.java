package io.xpipe.app.store;

import io.xpipe.app.fs.FileSystem;

public interface FileSystemStore extends DataStore {

    FileSystem createFileSystem() throws Exception;
}
