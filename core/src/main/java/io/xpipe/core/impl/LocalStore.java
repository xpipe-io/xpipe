package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.*;
import io.xpipe.core.util.JacksonizedValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements FileSystemStore, MachineStore {

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public FileSystem createFileSystem() {
        if (true) return new ConnectionFileSystem(ShellStore.local().create());
        return new FileSystem() {
            @Override
            public FileSystem open() throws Exception {
            return this;
            }

            @Override
            public boolean exists(String file) {
                return Files.exists(Path.of(file));
            }

            @Override
            public void delete(String file) throws Exception {

            }

            @Override
            public void copy(String file, String newFile) throws Exception {

            }

            @Override
            public void move(String file, String newFile) throws Exception {

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
            public void touch(String file) throws Exception {
            }

            @Override
            public boolean isDirectory(String file) throws Exception {
                return Files.isDirectory(Path.of(file));
            }

            @Override
            public Stream<FileEntry> listFiles(String file) throws Exception {
                return null;
            }

            @Override
            public List<String> listRoots() throws Exception {
                return null;
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
            public void close() throws IOException {

            }
        };
    }

    @Override
    public ShellProcessControl createControl() {
        return ProcessControlProvider.createLocal();
    }
}
