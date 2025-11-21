package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
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
    public boolean writeInstantIfPossible(FileSystem sourceFs, FilePath sourceFile, FilePath targetFile) throws Exception {
        return false;
    }

    @Override
    public boolean readInstantIfPossible(FilePath sourceFile, FileSystem targetFs, FilePath targetFile) throws Exception {
        return false;
    }

    @Override
    public String getSuffix() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return shellControl.isRunning(true);
    }

    @Override
    public boolean supportsLinkCreation() {
        return shellControl.getOsType() != OsType.WINDOWS;
    }

    @Override
    public boolean supportsOwnerColumn() {
        return shellControl.getOsType() != OsType.WINDOWS && shellControl.getOsType() != OsType.MACOS;
    }

    @Override
    public boolean supportsModeColumn() {
        return shellControl.getOsType() != OsType.WINDOWS;
    }

    @Override
    public boolean supportsDirectorySizes() {
        return true;
    }

    @Override
    public boolean supportsChmod() {
        return shellControl.getOsType() != OsType.WINDOWS;
    }

    @Override
    public boolean supportsChown() {
        return shellControl.getOsType() != OsType.WINDOWS && shellControl.getOsType() != OsType.MACOS;
    }

    @Override
    public boolean supportsChgrp() {
        return shellControl.getOsType() != OsType.WINDOWS && shellControl.getOsType() != OsType.MACOS;
    }

    @Override
    public boolean supportsTerminalWorkingDirectory() {
        return true;
    }

    @Override
    public Optional<ShellControl> getRawShellControl() {
        return Optional.of(shellControl);
    }

    @Override
    public void chmod(FilePath path, String mode, boolean recursive) throws Exception {
        shellControl
                .command(CommandBuilder.of()
                        .add("chmod")
                        .addIf(recursive, "-R")
                        .addLiteral(mode)
                        .addFile(path))
                .execute();
    }

    @Override
    public void chown(FilePath path, String uid, boolean recursive) throws Exception {
        shellControl
                .command(CommandBuilder.of()
                        .add("chown")
                        .addIf(recursive, "-R")
                        .addLiteral(uid)
                        .addFile(path))
                .execute();
    }

    @Override
    public void chgrp(FilePath path, String gid, boolean recursive) throws Exception {
        shellControl
                .command(CommandBuilder.of()
                        .add("chgrp")
                        .addIf(recursive, "-R")
                        .addLiteral(gid)
                        .addFile(path))
                .execute();
    }

    @Override
    public void kill() {
        shellControl.killExternal();
    }

    @Override
    public void cd(FilePath dir) throws Exception {
        shellControl.view().cd(dir);
    }

    @Override
    public boolean requiresReinit() {
        return !shellControl.isRunning(true) || shellControl.isAnyStreamClosed();
    }

    @Override
    public void reinitIfNeeded() throws Exception {
        shellControl.start();
        if (shellControl.isAnyStreamClosed()) {
            shellControl.restart();
        }
    }

    @Override
    public String getFileSeparator() {
        return OsFileSystem.of(shellControl.getOsType()).getFileSystemSeparator();
    }

    @Override
    public FilePath makeFileSystemCompatible(FilePath filePath) {
        return OsFileSystem.of(shellControl.getOsType()).makeFileSystemCompatible(filePath);
    }

    @Override
    public Optional<FilePath> pwd() throws Exception {
        return Optional.ofNullable(shellControl.view().pwd());
    }

    @Override
    public FileSystem createTransferOptimizedFileSystem() throws Exception {
        // For local, we have our optimized streams regardless
        if (!shellControl.isLocal() && shellControl.getShellDialect() == ShellDialects.CMD) {
            var pwsh = shellControl
                    .view()
                    .findProgram(ShellDialects.POWERSHELL_CORE.getExecutableName())
                    .isPresent();
            if (pwsh) {
                return new ConnectionFileSystem(
                        shellControl.subShell(ShellDialects.POWERSHELL_CORE).start());
            }

            var powershell = shellControl
                    .view()
                    .findProgram(ShellDialects.POWERSHELL.getExecutableName())
                    .isPresent();
            if (powershell) {
                return new ConnectionFileSystem(
                        shellControl.subShell(ShellDialects.POWERSHELL).start());
            }
        }

        return this;
    }

    @Override
    public long getFileSize(FilePath file) throws Exception {
        return Long.parseLong(shellControl
                .getShellDialect()
                .queryFileSize(shellControl, file.toString())
                .readStdoutOrThrow());
    }

    @Override
    public long getDirectorySize(FilePath file) throws Exception {
        return shellControl.getShellDialect().queryDirectorySize(shellControl, file.toString());
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
            try {
                d.throwIfUnsupported();
            } catch (Exception e) {
                throw ErrorEventFactory.expected(e);
            }
        }

        if (!shellControl.getTtyState().isPreservesOutput()
                || !shellControl.getTtyState().isSupportsInput()) {
            var ex = new UnsupportedOperationException(
                    "Shell has a PTY allocated and as a result does not support file system operations.");
            ErrorEventFactory.preconfigure(
                    ErrorEventFactory.fromThrowable(ex).documentationLink(DocumentationLink.TTY));
            throw ex;
        }

        shellControl.checkLicenseOrThrow();

        return this;
    }

    @Override
    public InputStream openInput(FilePath file) throws Exception {
        if (shellControl.isLocal()) {
            return new BufferedInputStream(Files.newInputStream(file.asLocalPath()));
        }

        return shellControl
                .getShellDialect()
                .getFileReadCommand(shellControl, file.toString())
                .startExternalStdout();
    }

    @Override
    public OutputStream openOutput(FilePath file, long totalBytes) throws Exception {
        if (shellControl.isLocal()) {
            return new BufferedOutputStream(Files.newOutputStream(file.asLocalPath()));
        }

        var cmd =
                shellControl.getShellDialect().createStreamFileWriteCommand(shellControl, file.toString(), totalBytes);
        cmd.setExitTimeout(Duration.ofMillis(Long.MAX_VALUE));
        return cmd.startExternalStdin();
    }

    @Override
    public boolean fileExists(FilePath file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .createFileExistsCommand(shellControl, file.toString())
                .start()) {
            return pc.discardAndCheckExit();
        }
    }

    @Override
    public void delete(FilePath file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .deleteFileOrDirectory(shellControl, file.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void copy(FilePath file, FilePath newFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileCopyCommand(shellControl, file.toString(), newFile.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void move(FilePath file, FilePath newFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileMoveCommand(shellControl, file.toString(), newFile.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void mkdirs(FilePath file) throws Exception {
        try (var pc = shellControl
                .command(
                        CommandBuilder.ofFunction(proc -> proc.getShellDialect().getMkdirsCommand(file.toString())))
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void touch(FilePath file) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .getFileTouchCommand(shellControl, file.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public void symbolicLink(FilePath linkFile, FilePath targetFile) throws Exception {
        try (var pc = shellControl
                .getShellDialect()
                .symbolicLink(shellControl, linkFile.toString(), targetFile.toString())
                .start()) {
            pc.discardOrThrow();
        }
    }

    @Override
    public boolean directoryExists(FilePath file) throws Exception {
        return shellControl
                .getShellDialect()
                .directoryExists(shellControl, file.toString())
                .executeAndCheck();
    }

    @Override
    public void directoryAccessible(FilePath file) throws Exception {
        var current = shellControl.executeSimpleStringCommand(
                shellControl.getShellDialect().getPrintWorkingDirectoryCommand());
        shellControl
                .command(shellControl.getShellDialect().getCdCommand(file.toString()))
                .execute();
        shellControl
                .command(shellControl.getShellDialect().getCdCommand(current))
                .execute();
    }

    @Override
    public Optional<FileEntry> getFileInfo(FilePath file) throws Exception {
        try (var stream = shellControl.getShellDialect().listFiles(this, shellControl, file.toString(), false)) {
            var l = stream.toList();
            return l.stream().findFirst();
        }
    }

    @Override
    public Stream<FileEntry> listFiles(FileSystem system, FilePath file) throws Exception {
        return shellControl.getShellDialect().listFiles(system, shellControl, file.toString(), true);
    }

    @Override
    public List<FilePath> listRoots() throws Exception {
        return shellControl
                .getShellDialect()
                .listRoots(shellControl)
                .map(s -> FilePath.of(s))
                .toList();
    }

    @Override
    public List<FilePath> listCommonDirectories() throws Exception {
        var home = shellControl.view().userHome();
        if (shellControl.getOsType() == OsType.WINDOWS) {
            return List.of(home, home.join("Documents"), home.join("Downloads"), home.join("Desktop"));
        } else if (shellControl.getOsType() == OsType.MACOS) {
            var list = List.of(
                    home,
                    home.join("Downloads"),
                    home.join("Documents"),
                    home.join("Desktop"),
                    FilePath.of("/Applications"),
                    FilePath.of("/Library"),
                    FilePath.of("/System"),
                    FilePath.of("/etc"),
                    FilePath.of("/tmp"));
            return list;
        } else {
            var list = new ArrayList<>(List.of(
                    home,
                    home.join("Downloads"),
                    home.join("Documents"),
                    FilePath.of("/etc"),
                    shellControl.getSystemTemporaryDirectory(),
                    FilePath.of("/var")));
            var parentHome = home.getParent();
            if (parentHome != null && !parentHome.toString().equals("/")) {
                list.add(3, parentHome);
            }
            return list;
        }
    }

    @Override
    public void close() {
        // In case the shell control is already in an invalid state, this operation might fail
        // Since we are only closing, just swallow all exceptions
        try {
            shellControl.close();
        } catch (Exception ex) {
            ErrorEventFactory.fromThrowable(ex).omit().expected().handle();
        }
    }
}
