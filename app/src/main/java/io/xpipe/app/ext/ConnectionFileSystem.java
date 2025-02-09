package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.FileEntry;
import io.xpipe.core.store.FileSystem;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Getter
public class ConnectionFileSystem implements FileSystem {

    @JsonIgnore
    protected final ShellControl shellControl;

    public ConnectionFileSystem(ShellControl shellControl) {
        this.shellControl = shellControl;
    }

    @Override
    public long getFileSize(String file) throws Exception {
        return Long.parseLong(
                shellControl.getShellDialect().queryFileSize(shellControl, file).readStdoutOrThrow());
    }

    @Override
    public Optional<ShellControl> getShell() {
        return Optional.of(shellControl);
    }

    @Override
    public FileSystem open() throws Exception {
        shellControl.start();

        var d = shellControl.getShellDialect().getDumbMode();
        if (!d.supportsAnyPossibleInteraction()) {
            shellControl.close();
            d.throwIfUnsupported();
        }

        if (!shellControl.getTtyState().isPreservesOutput()
                || !shellControl.getTtyState().isSupportsInput()) {
            throw new UnsupportedOperationException(
                    "Shell has a PTY allocated and as a result does not support file system operations");
        }

        shellControl.checkLicenseOrThrow();

        return this;
    }

    @Override
    public InputStream openInput(String file) throws Exception {
        return shellControl
                .getShellDialect()
                .getFileReadCommand(shellControl, file)
                .startExternalStdout();
    }

    @Override
    public OutputStream openOutput(String file, long totalBytes) throws Exception {
        var cmd = shellControl.getShellDialect().createStreamFileWriteCommand(shellControl, file, totalBytes);
        cmd.setExitTimeout(Duration.ofMillis(Long.MAX_VALUE));
        return cmd.startExternalStdin();
    }

    @Override
    public boolean fileExists(String file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .createFileExistsCommand(shellControl, file)
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void delete(String file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .deleteFileOrDirectory(shellControl, file)
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void copy(String file, String newFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileCopyCommand(shellControl, file, newFile)
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void move(String file, String newFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileMoveCommand(shellControl, file, newFile)
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void mkdirs(String file) throws Exception {
        try (var pc = shellControl
                .command(
                        CommandBuilder.ofFunction(proc -> proc.getShellDialect().getMkdirsCommand(file)))
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void touch(String file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileTouchCommand(shellControl, file)
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void symbolicLink(String linkFile, String targetFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .symbolicLink(shellControl, linkFile, targetFile)
                .start()) {
            pc.discardOrThrow();
        }
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
        var current = shellControl.executeSimpleStringCommand(
                shellControl.getShellDialect().getPrintWorkingDirectoryCommand());
        shellControl.command(shellControl.getShellDialect().getCdCommand(file));
        shellControl.command(shellControl.getShellDialect().getCdCommand(current));
    }

    @Override
    public Stream<FileEntry> listFiles(String file) throws Exception {
        return shellControl.getShellDialect().listFiles(this, shellControl, file);
    }

    @Override
    public List<String> listRoots() throws Exception {
        return shellControl.getShellDialect().listRoots(shellControl).toList();
    }

    @Override
    public void close() {
        // In case the shell control is already in an invalid state, this operation might fail
        // Since we are only closing, just swallow all exceptions
        try {
            shellControl.close();
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).omit().expected().handle();
        }
    }
}
