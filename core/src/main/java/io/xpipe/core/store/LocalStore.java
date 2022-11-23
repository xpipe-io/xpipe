package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.util.JacksonizedValue;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

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
    public ShellProcessControl create() {
        return LocalProcessControlProvider.create();
    }

    public static abstract class LocalProcessControlProvider {

        private  static  LocalProcessControlProvider INSTANCE;

        public static void init(ModuleLayer layer) {
            INSTANCE = ServiceLoader.load(layer, LocalProcessControlProvider.class).findFirst().orElseThrow();
        }

        public static ShellProcessControl create() {
            return INSTANCE.createProcessControl();
        }

        public abstract ShellProcessControl createProcessControl();
    }
}
