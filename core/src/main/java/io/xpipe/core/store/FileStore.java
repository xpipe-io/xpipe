package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Represents a file located on a certain machine.
 */
@Value
@JsonTypeName("file")
public class FileStore implements StreamDataStore, FilenameStore {

    public static FileStore local(Path p) {
        return new FileStore(new LocalStore(), p.toString());
    }

    /**
     * Creates a file store for a file that is local to the callers machine.
     */
    public static FileStore local(String p) {
        return new FileStore(new LocalStore(), p);
    }

    MachineFileStore machine;
    String file;

    @JsonCreator
    public FileStore(MachineFileStore machine, String file) {
        this.machine = machine;
        this.file = file;
    }

    @Override
    public void validate() throws Exception {
        if (machine == null) {
            throw new IllegalStateException("Machine is missing");
        }
        if (file == null) {
            throw new IllegalStateException("File is missing");
        }

    }

    @Override
    public InputStream openInput() throws Exception {
        return machine.openInput(file);
    }

    @Override
    public OutputStream openOutput() throws Exception {
        return machine.openOutput(file);
    }

    @Override
    public boolean canOpen() throws Exception {
        return machine.exists(file);
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
