package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.JacksonizedValue;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Represents a file located on a file system.
 */
@JsonTypeName("file")
@SuperBuilder
@Jacksonized
@Getter
public class FileStore extends JacksonizedValue implements FilenameStore, StreamDataStore {

    FileSystemStore fileSystem;
    String file;

    public FileStore(FileSystemStore fileSystem, String file) {
        this.fileSystem = fileSystem;
        this.file = file;
    }

    public final boolean isLocal() {
        return fileSystem instanceof LocalStore;
    }

    public static FileStore local(Path p) {
        return new FileStore(new LocalStore(), p.toString());
    }

    /**
     * Creates a file store for a file that is local to the callers machine.
     */
    public static FileStore local(String p) {
        return new FileStore(new LocalStore(), p);
    }

    @Override
    public void checkComplete() throws Exception {
        if (fileSystem == null) {
            throw new IllegalStateException("Machine is missing");
        }
        if (file == null) {
            throw new IllegalStateException("File is missing");
        }
    }

    @Override
    public InputStream openInput() throws Exception {
        return fileSystem.openInput(file);
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return fileSystem.openOutput(file);
    }

    @Override
    public boolean canOpen() throws Exception {
        return fileSystem.exists(file);
    }

    @Override
    public String getFileName() {
        var split = file.split("[\\\\/]");
        if (split.length == 0) {
            return "";
        }
        return split[split.length - 1];
    }
}
