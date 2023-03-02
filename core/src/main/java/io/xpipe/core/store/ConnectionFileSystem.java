package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.xpipe.core.process.ShellControl;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
public class ConnectionFileSystem implements FileSystem {

    @JsonIgnore
    private final ShellControl shellControl;

    @JsonIgnore
    private final ShellStore store;

    public ConnectionFileSystem(ShellControl shellControl, ShellStore store) {
        this.shellControl = shellControl;
        this.store = store;
    }

    @Override
    public List<String> listRoots() throws Exception {
        return shellControl.getShellDialect().listRoots(shellControl).toList();
    }

    @Override
    public boolean isDirectory(String file) throws Exception{return true;}

    @Override
    public Stream<FileEntry> listFiles(String file) throws Exception {
        return shellControl.getShellDialect().listFiles(this, shellControl, file);
    }

    @Override
    public FileSystemStore getStore() {
        return store;
    }

    @Override
    public Optional<ShellControl> getShell() {
        return Optional.of(shellControl);
    }

    @Override
    public FileSystem open() throws Exception {
        shellControl.start();
        return this;
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        return shellControl.command(proc ->
                                        proc.getShellDialect().getFileReadCommand(proc.getOsType().normalizeFileName(file)))
                .startExternalStdout();
    }

    @Override
    public OutputStream openOutput(String file) throws Exception {
        return shellControl.getShellDialect()
                        .getStreamFileWriteCommand(shellControl, shellControl.getOsType().normalizeFileName(file))
                .startExternalStdin();
    }

    @Override
    public boolean exists(String file) throws Exception {
        try (var pc = shellControl.command(proc -> proc.getShellDialect()
                        .getFileExistsCommand(proc.getOsType().normalizeFileName(file)))
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void delete(String file) throws Exception {
        try (var pc = shellControl.command(proc -> proc.getShellDialect()
                        .getFileDeleteCommand(proc.getOsType().normalizeFileName(file)))
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void copy(String file, String newFile) throws Exception {
        try (var pc = shellControl.command(proc -> proc.getShellDialect()
                        .getFileCopyCommand(proc.getOsType().normalizeFileName(file), proc.getOsType().normalizeFileName(newFile))).complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void move(String file, String newFile) throws Exception {
        try (var pc = shellControl.command(proc -> proc.getShellDialect()
                        .getFileMoveCommand(proc.getOsType().normalizeFileName(file), proc.getOsType().normalizeFileName(newFile))).complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public boolean mkdirs(String file) throws Exception {
        try (var pc = shellControl.command(proc -> proc.getShellDialect()
                        .flatten(proc.getShellDialect()
                                         .getMkdirsCommand(proc.getOsType().normalizeFileName(file))))
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void touch(String file) throws Exception {
        try (var pc = shellControl.command(proc -> proc.getShellDialect()
                        .getFileTouchCommand(proc.getOsType().normalizeFileName(file))).complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void close() throws IOException {
        shellControl.close();
    }
}
