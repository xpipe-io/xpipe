package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.FilenameStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.core.util.ValidationException;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Represents a file located on a file system.
 */
@JsonTypeName("file")
@SuperBuilder
@Jacksonized
@Getter
public class FileStore extends JacksonizedValue implements FilenameStore, StreamDataStore {

    FileSystemStore fileSystem;
    String path;

    public FileStore(FileSystemStore fileSystem, String path) {
        this.fileSystem = fileSystem;
        this.path = path;
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

    public String getParent() {
        var matcher = Pattern.compile("^(.+?)[^\\\\/]+$").matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unable to determine parent of " + path);
        }

        return matcher.group(1);
    }

    public final boolean isLocal() {
        return fileSystem instanceof LocalStore;
    }

    @Override
    public void checkComplete() throws Exception {
        if (fileSystem == null) {
            throw new ValidationException("File system is missing");
        }
        if (path == null) {
            throw new ValidationException("File is missing");
        }
        if (!FileNames.isAbsolute(path)) {
            throw new ValidationException("File path is not absolute");
        }
    }

    @Override
    public InputStream openInput() throws Exception {
        return fileSystem.createFileSystem().open().openInput(path);
    }

    @Override
    public OutputStream openOutput() throws Exception {
        fileSystem.createFileSystem().open().mkdirs(getParent());
        return fileSystem.createFileSystem().open().openOutput(path);
    }

    @Override
    public boolean canOpen() throws Exception {
        return fileSystem.createFileSystem().open().exists(path);
    }

    @Override
    public String getFileName() {
        var split = path.split("[\\\\/]");
        if (split.length == 0) {
            return "";
        }
        return split[split.length - 1];
    }
}
