package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Value;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

@Value
@JsonTypeName("file")
public class FileStore implements StreamDataStore, FilenameStore {

    public static FileStore local(Path p) {
        return new FileStore(MachineFileStore.local(), p.toString());
    }

    public static FileStore local(String p) {
        return new FileStore(MachineFileStore.local(), p);
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
        if (!machine.exists(file)) {
            throw new IllegalStateException("File " + file + " could not be found on machine " + machine.toDisplay());
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
    public String toDisplay() {
        return file + "@" + machine.toDisplay();
    }

    @Override
    public boolean persistent() {
        return true;
    }

    @Override
    public String getFileName() {
        return file;
    }
}
