package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellProcessControl;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.MachineStore;
import io.xpipe.core.util.JacksonizedValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements FileSystemStore, MachineStore {

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public FileSystem createFileSystem() {
        return new FileSystem() {
            @Override
            public Optional<ShellProcessControl> getShell() {
                return Optional.empty();
            }

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
                Files.delete(Path.of(file));
            }

            @Override
            public void copy(String file, String newFile) throws Exception {
                Files.copy(Path.of(file), Path.of(newFile), StandardCopyOption.REPLACE_EXISTING);
            }

            @Override
            public void move(String file, String newFile) throws Exception {
                Files.move(Path.of(file), Path.of(newFile), StandardCopyOption.REPLACE_EXISTING);
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
                if (exists(file)) {
                    return;
                }

                Files.createFile(Path.of(file));
            }

            @Override
            public boolean isDirectory(String file) throws Exception {
                return Files.isDirectory(Path.of(file));
            }

            @Override
            public Stream<FileEntry> listFiles(String file) throws Exception {
                return Files.list(Path.of(file)).map(path -> {
                    try {
                        var date = Files.getLastModifiedTime(path);
                        var size = Files.isDirectory(path) ? 0 : Files.size(path);
                        return new FileEntry(
                                this,
                                path.toString(),
                                date.toInstant(),
                                Files.isDirectory(path),
                                Files.isHidden(path),
                                Files.isExecutable(path),
                                size);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }

            @Override
            public List<String> listRoots() throws Exception {
                return StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), false).map(path -> path.toString()).toList();
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
            public void close() throws IOException {}
        };
    }

    @Override
    public ShellProcessControl createControl() {
        return ProcessControlProvider.createLocal();
    }
}
