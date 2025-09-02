package io.xpipe.app.ext;

import io.xpipe.app.process.ShellControl;
import io.xpipe.core.FilePath;

import io.xpipe.core.OsType;
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
    private final Supplier<Boolean> check;

    public WrapperFileSystem(FileSystem fs, Supplier<Boolean> check) {
        this.fs = fs;
        this.check = check;
    }

    @Override
    public void reinitIfNeeded() throws Exception {
        fs.reinitIfNeeded();
    }

    @Override
    public String getFileSeparator() {
        return fs.getFileSeparator();
    }

    @Override
    public Optional<OsType> getOsType() {
        return fs.getOsType();
    }

    @Override
    public Optional<FilePath> pwd() throws Exception {
        return fs.pwd();
    }

    @Override
    public FileSystem createTransferOptimizedFileSystem() {
        return this;
    }

    @Override
    public long getFileSize(FilePath file) throws Exception {
        if (!check.get()) {
            return 0;
        }

        return fs.getFileSize(file);
    }

    @Override
    public long getDirectorySize(FilePath file) throws Exception {
        if (!check.get()) {
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
        if (!check.get()) {
            return InputStream.nullInputStream();
        }

        return fs.openInput(file);
    }

    @Override
    public OutputStream openOutput(FilePath file, long totalBytes) throws Exception {
        if (!check.get()) {
            return OutputStream.nullOutputStream();
        }

        return fs.openOutput(file, totalBytes);
    }

    @Override
    public boolean fileExists(FilePath file) throws Exception {
        if (!check.get()) {
            return false;
        }

        return fs.fileExists(file);
    }

    @Override
    public void delete(FilePath file) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.delete(file);
    }

    @Override
    public void copy(FilePath file, FilePath newFile) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.copy(file, newFile);
    }

    @Override
    public void move(FilePath file, FilePath newFile) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.move(file, newFile);
    }

    @Override
    public void mkdirs(FilePath file) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.mkdirs(file);
    }

    @Override
    public void touch(FilePath file) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.touch(file);
    }

    @Override
    public void symbolicLink(FilePath linkFile, FilePath targetFile) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.symbolicLink(linkFile, targetFile);
    }

    @Override
    public boolean directoryExists(FilePath file) throws Exception {
        if (!check.get()) {
            return false;
        }

        return fs.directoryExists(file);
    }

    @Override
    public void directoryAccessible(FilePath file) throws Exception {
        if (!check.get()) {
            return;
        }

        fs.directoryAccessible(file);
    }

    @Override
    public Optional<FileEntry> getFileInfo(FilePath file) throws Exception {
        if (!check.get()) {
            return Optional.empty();
        }

        return fs.getFileInfo(file);
    }

    @Override
    public Stream<FileEntry> listFiles(FileSystem system, FilePath file) throws Exception {
        if (!check.get()) {
            return Stream.empty();
        }

        return fs.listFiles(system, file);
    }

    @Override
    public List<FilePath> listRoots() throws Exception {
        if (!check.get()) {
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
