package io.xpipe.app.ext;

public interface FileSystemStore extends DataStore {

    FileSystem createFileSystem() throws Exception;
}
