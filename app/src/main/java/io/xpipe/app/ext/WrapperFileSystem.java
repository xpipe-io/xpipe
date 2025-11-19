package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
public class WrapperFileSystem implements FileSystem {

    private final FileSystem fs;
    private final Supplier<Boolean> runningCheck;

    public WrapperFileSystem(FileSystem fs, Supplier<Boolean> runningCheck) {
        this.fs = fs;
        this.runningCheck = runningCheck;
    }

    @Override
    public boolean writeInstantIfPossible(FileSystem sourceFs, FilePath sourceFile, FilePath targetFile) throws Exception {
        return fs.writeInstantIfPossible(sourceFs, sourceFile, targetFile);
    }

    @Override
    public String getSuffix() {
        return fs.getSuffix();
    }

    @Override
    public boolean isRunning() {
        return fs.isRunning();
    }

    @Override
    public boolean supportsLinkCreation() {
        return fs.supportsLinkCreation();
    }

    @Override
    public boolean supportsOwnerColumn() {
        return fs.supportsOwnerColumn();
    }

    @Override
    public boolean supportsModeColumn() {
        return fs.supportsModeColumn();
    }

    @Override
    public boolean supportsDirectorySizes() {
        return fs.supportsDirectorySizes();
    }

    @Override
    public boolean supportsChmod() {
        return fs.supportsChmod();
    }

    @Override
    public boolean supportsChown() {
        return fs.supportsChown();
    }

    @Override
    public boolean supportsChgrp() {
        return fs.supportsChgrp();
    }

    @Override
    public boolean supportsTerminalWorkingDirectory() {
        return fs.supportsTerminalWorkingDirectory();
    }

    @Override
    public Optional<ShellControl> getRawShellControl() {
        return fs.getRawShellControl();
    }

    @Override
    public void chmod(FilePath path, String mode, boolean recursive) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.chmod(path, mode, recursive);
    }

    @Override
    public void chown(FilePath path, String uid, boolean recursive) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.chown(path, uid, recursive);
    }

    @Override
    public void chgrp(FilePath path, String gid, boolean recursive) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.chgrp(path, gid, recursive);
    }

    @Override
    public void kill() {
        if (!runningCheck.get()) {
            return;
        }

        fs.kill();
    }

    @Override
    public void cd(FilePath dir) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.cd(dir);
    }

    @Override
    public boolean requiresReinit() {
        if (!runningCheck.get()) {
            return false;
        }

        return fs.requiresReinit();
    }

    @Override
    public void reinitIfNeeded() throws Exception {
        fs.reinitIfNeeded();
    }

    @Override
    public String getFileSeparator() {
        if (!runningCheck.get()) {
            return "/";
        }

        return fs.getFileSeparator();
    }

    @Override
    public FilePath makeFileSystemCompatible(FilePath filePath) {
        if (!runningCheck.get()) {
            return filePath;
        }

        return fs.makeFileSystemCompatible(filePath);
    }

    @Override
    public Optional<FilePath> pwd() throws Exception {
        if (!runningCheck.get()) {
            return Optional.empty();
        }

        return fs.pwd();
    }

    @Override
    public FileSystem createTransferOptimizedFileSystem() {
        return this;
    }

    @Override
    public long getFileSize(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return 0;
        }

        return fs.getFileSize(file);
    }

    @Override
    public long getDirectorySize(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return 0;
        }

        return fs.getDirectorySize(file);
    }

    @Override
    public Optional<ShellControl> getShell() {
        return fs.getShell();
    }

    @Override
    public FileSystem open() throws Exception {
        return fs.open();
    }

    @Override
    public InputStream openInput(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return InputStream.nullInputStream();
        }

        return fs.openInput(file);
    }

    @Override
    public OutputStream openOutput(FilePath file, long totalBytes) throws Exception {
        if (!runningCheck.get()) {
            return OutputStream.nullOutputStream();
        }

        return fs.openOutput(file, totalBytes);
    }

    @Override
    public boolean fileExists(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return false;
        }

        return fs.fileExists(file);
    }

    @Override
    public void delete(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.delete(file);
    }

    @Override
    public void copy(FilePath file, FilePath newFile) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.copy(file, newFile);
    }

    @Override
    public void move(FilePath file, FilePath newFile) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.move(file, newFile);
    }

    @Override
    public void mkdirs(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.mkdirs(file);
    }

    @Override
    public void touch(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.touch(file);
    }

    @Override
    public void symbolicLink(FilePath linkFile, FilePath targetFile) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.symbolicLink(linkFile, targetFile);
    }

    @Override
    public boolean directoryExists(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return false;
        }

        return fs.directoryExists(file);
    }

    @Override
    public void directoryAccessible(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return;
        }

        fs.directoryAccessible(file);
    }

    @Override
    public Optional<FileEntry> getFileInfo(FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return Optional.empty();
        }

        return fs.getFileInfo(file);
    }

    @Override
    public Stream<FileEntry> listFiles(FileSystem system, FilePath file) throws Exception {
        if (!runningCheck.get()) {
            return Stream.empty();
        }

        return fs.listFiles(system, file);
    }

    @Override
    public List<FilePath> listRoots() throws Exception {
        if (!runningCheck.get()) {
            return List.of();
        }

        return fs.listRoots();
    }

    @Override
    public List<FilePath> listCommonDirectories() throws Exception {
        return fs.listCommonDirectories();
    }

    @Override
    public void close() throws IOException {
        fs.close();
    }
}
