package io.xpipe.core.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.process.ProcessControlProvider;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.ConnectionFileSystem;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.FileSystemStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;

import java.nio.file.Path;

@JsonTypeName("local")
public class LocalStore extends JacksonizedValue implements ShellStore {

    private static ShellControl local;
    private static FileSystem localFileSystem;

    public static ShellControl getShell() throws Exception {
        if (local == null) {
            local = ProcessControlProvider.createLocal(false).start();
        }

        return local;
    }

    public static FileSystem getFileSystem() throws Exception {
        if (localFileSystem == null) {
            localFileSystem = new LocalStore().createFileSystem();
        }

        return localFileSystem;
    }

    @Override
    public FileSystem createFileSystem() {
        return new ConnectionFileSystem(ShellStore.createLocal().control(), LocalStore.this) {

            @Override
            public FileSystemStore getStore() {
                return LocalStore.this;
            }


            private Path wrap(String file) {
                for (var e : System.getenv().entrySet()) {
                    file = file.replace(
                            ShellDialects.getPlatformDefault().environmentVariable(e.getKey()),
                            e.getValue());
                }
                return Path.of(file);
            }

//            @Override
//            public boolean exists(String file) {
//                return Files.exists(wrap(file));
//            }
//
//            @Override
//            public void delete(String file) throws Exception {
//                Files.delete(wrap(file));
//            }
//
//            @Override
//            public void copy(String file, String newFile) throws Exception {
//                Files.copy(wrap(file), wrap(newFile), StandardCopyOption.REPLACE_EXISTING);
//            }
//
//            @Override
//            public void move(String file, String newFile) throws Exception {
//                Files.move(wrap(file), wrap(newFile), StandardCopyOption.REPLACE_EXISTING);
//            }
//
//            @Override
//            public boolean mkdirs(String file) throws Exception {
//                try {
//                    Files.createDirectories(wrap(file));
//                    return true;
//                } catch (Exception ex) {
//                    return false;
//                }
//            }
//
//            @Override
//            public void touch(String file) throws Exception {
//                if (exists(file)) {
//                    return;
//                }
//
//                Files.createFile(wrap(file));
//            }
//
//            @Override
//            public boolean isDirectory(String file) throws Exception {
//                return Files.isDirectory(wrap(file));
//            }
//
//            @Override
//            public Stream<FileEntry> listFiles(String file) throws Exception {
//                return Files.list(wrap(file)).map(path -> {
//                    try {
//                        var date = Files.getLastModifiedTime(path);
//                        var size = Files.isDirectory(path) ? 0 : Files.size(path);
//                        return new FileEntry(
//                                this,
//                                path.toString(),
//                                date.toInstant(),
//                                Files.isDirectory(path),
//                                Files.isHidden(path),
//                                Files.isExecutable(path),
//                                size);
//                    } catch (IOException e) {
//                        throw new UncheckedIOException(e);
//                    }
//                });
//            }
//
//            @Override
//            public List<String> listRoots() throws Exception {
//                return StreamSupport.stream(
//                                FileSystems.getDefault().getRootDirectories().spliterator(), false)
//                        .map(path -> path.toString())
//                        .toList();
//            }
        };
    }

    @Override
    public ShellControl createBasicControl() {
        return ProcessControlProvider.createLocal(true);
    }
}
