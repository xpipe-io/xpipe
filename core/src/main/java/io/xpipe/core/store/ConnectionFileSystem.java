package io.xpipe.core.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.xpipe.core.process.ShellProcessControl;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class ConnectionFileSystem implements FileSystem {

    @JsonIgnore
    private final ShellProcessControl shellProcessControl;

    public ConnectionFileSystem(ShellProcessControl shellProcessControl) {
        this.shellProcessControl = shellProcessControl;
    }

    @Override
    public List<String> listRoots() throws Exception {
        return shellProcessControl.getShellType().listRoots(shellProcessControl).toList();
    }

    @Override
    public boolean isDirectory(String file) throws Exception{return true;}

    @Override
    public Stream<FileEntry> listFiles(String file) throws Exception {
        return shellProcessControl.getShellType().listFiles(this, shellProcessControl, file);
    }

    @Override
    public FileSystem open() throws Exception {
        shellProcessControl.start();
        return this;
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        return shellProcessControl.command(proc ->
                                        proc.getShellType().getFileReadCommand(proc.getOsType().normalizeFileName(file)))
                .startExternalStdout();
    }

    @Override
    public OutputStream openOutput(String file) throws Exception {
        return shellProcessControl.command(proc -> proc.getShellType()
                        .getStreamFileWriteCommand(proc.getOsType().normalizeFileName(file)))
                .startExternalStdin();
    }

    @Override
    public boolean exists(String file) throws Exception {
        try (var pc = shellProcessControl.command(proc -> proc.getShellType()
                        .getFileExistsCommand(proc.getOsType().normalizeFileName(file)))
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void delete(String file) throws Exception {
        try (var pc = shellProcessControl.command(proc -> proc.getShellType()
                        .getFileDeleteCommand(proc.getOsType().normalizeFileName(file)))
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void copy(String file, String newFile) throws Exception {
        try (var pc = shellProcessControl.command(proc -> proc.getShellType()
                        .getFileCopyCommand(proc.getOsType().normalizeFileName(file), proc.getOsType().normalizeFileName(newFile))).complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void move(String file, String newFile) throws Exception {
        try (var pc = shellProcessControl.command(proc -> proc.getShellType()
                        .getFileMoveCommand(proc.getOsType().normalizeFileName(file), proc.getOsType().normalizeFileName(newFile))).complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public boolean mkdirs(String file) throws Exception {
        try (var pc = shellProcessControl.command(proc -> proc.getShellType()
                        .flatten(proc.getShellType()
                                         .getMkdirsCommand(proc.getOsType().normalizeFileName(file))))
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void touch(String file) throws Exception {
        try (var pc = shellProcessControl.command(proc -> proc.getShellType()
                        .getFileTouchCommand(proc.getOsType().normalizeFileName(file))).complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void close() throws IOException {
        shellProcessControl.close();
    }
}
