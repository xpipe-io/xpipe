package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.util.JacksonizedValue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements FileSystemStore, MachineStore {

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public boolean exists(String file) {
        return Files.exists(Path.of(file));
    }

    @Override
    public boolean mkdirs(String file) throws Exception {
        try {
            Files.createDirectories(Path.of(file));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        var p = Path.of(file);
        return Files.newInputStream(p);
    }

    @Override
    public OutputStream openOutput(String file) throws Exception {
        var p = Path.of(file);
        return Files.newOutputStream(p);
    }

    @Override
    public ShellProcessControl createControl() {
        return ProcessControlProvider.createLocal();
    }
}
