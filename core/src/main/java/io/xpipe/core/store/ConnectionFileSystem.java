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
    protected final ShellControl shellControl;

    @JsonIgnore
    protected final ShellStore store;

    public ConnectionFileSystem(ShellControl shellControl, ShellStore store) {
        this.shellControl = shellControl;
        this.store = store;
    }

    @Override
    public List<String> listRoots() throws Exception {
        return shellControl.getShellDialect().listRoots(shellControl).toList();
    }

    @Override
    public boolean directoryExists(String file) throws Exception {
        return shellControl
                .getShellDialect()
                .directoryExists(shellControl, file)
                .executeAndCheck();
    }

    @Override
    public void directoryAccessible(String file) throws Exception {
        shellControl.executeSimpleCommand(shellControl.getShellDialect().getCdCommand(file));
    }

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
        return shellControl
                .command(proc -> proc.getShellDialect().getFileReadCommand(file))
                .startExternalStdout();
    }

    @Override
    public OutputStream openOutput(String file) throws Exception {
        return shellControl
                .getShellDialect()
                .createStreamFileWriteCommand(shellControl, file)
                .startExternalStdin();
    }

    @Override
    public boolean fileExists(String file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .createFileExistsCommand(shellControl, file)
                .complex()
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void delete(String file) throws Exception {
        try (var pc = shellControl
                .command(proc -> proc.getShellDialect().getFileDeleteCommand(file))
                .complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void copy(String file, String newFile) throws Exception {
        try (var pc = shellControl
                .command(proc -> proc.getShellDialect().getFileCopyCommand(file, newFile))
                .complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void move(String file, String newFile) throws Exception {
        try (var pc = shellControl
                .command(proc -> proc.getShellDialect().getFileMoveCommand(file, newFile))
                .complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void mkdirs(String file) throws Exception {
        try (var pc = shellControl
                .command(proc -> proc.getShellDialect().getMkdirsCommand(file))
                .complex()
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void touch(String file) throws Exception {
        try (var pc = shellControl
                .command(proc -> proc.getShellDialect().getFileTouchCommand(file))
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void symbolicLink(String linkFile, String targetFile) throws Exception {
        try (var pc = shellControl.getShellDialect().symbolicLink(shellControl,linkFile, targetFile)
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void close() throws IOException {
        shellControl.close();
    }
}
