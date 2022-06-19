package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@JsonTypeName("local")
public class LocalMachineStore implements MachineStore {

    @Override
    public boolean exists(String file) {
        return Files.exists(Path.of(file));
    }

    @Override
    public String toDisplay() {
        return "local";
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
}
