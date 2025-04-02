package io.xpipe.core.process;

import io.xpipe.core.store.FilePath;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ShellView {

    protected final ShellControl shellControl;

    public ShellView(ShellControl shellControl) {
        this.shellControl = shellControl;
    }

    protected ShellDialect getDialect() {
        return shellControl.getShellDialect();
    }

    public FilePath writeTempTextFileDeterministic(String fileName, String text) throws Exception {
        var hash = Math.abs(text.hashCode());
        var f = FilePath.of(fileName);
        var target = FilePath.of(f.getBaseName().toString() + "-" + hash + f.getExtension());
        if (fileExists(target)) {
            return target;
        }
        writeTextFile(target, text);
        return target;
    }

    public byte[] readRawFile(FilePath path) throws Exception {
        var s = getDialect().getFileReadCommand(shellControl, path.toString()).readRawBytesOrThrow();
        return s;
    }

    public String readTextFile(FilePath path) throws Exception {
        var s = getDialect().getFileReadCommand(shellControl, path.toString()).readStdoutOrThrow();
        return s;
    }

    public void writeTextFile(FilePath path, String text) throws Exception {
        var cc = getDialect().createTextFileWriteCommand(shellControl, text, path.toString());
        cc.execute();
    }

    public void writeScriptFile(FilePath path, String text) throws Exception {
        var cc = getDialect().createScriptTextFileWriteCommand(shellControl, text, path.toString());
        cc.execute();
    }

    public void writeStreamFile(FilePath path, InputStream inputStream, long size) throws Exception {
        try (var out = getDialect()
                .createStreamFileWriteCommand(shellControl, path.toString(), size)
                .startExternalStdin()) {
            inputStream.transferTo(out);
        }
    }

    public FilePath userHome() throws Exception {
        return FilePath.of(shellControl.getOsType().getUserHomeDirectory(shellControl));
    }

    public boolean fileExists(FilePath path) throws Exception {
        return getDialect()
                .createFileExistsCommand(shellControl, path.toString())
                .executeAndCheck();
    }

    public void mkdir(FilePath path) throws Exception {
        shellControl.command(getDialect()
                .getMkdirsCommand(path.toString()))
                .execute();
    }

    public boolean directoryExists(FilePath path) throws Exception {
        return getDialect().directoryExists(shellControl, path.toString()).executeAndCheck();
    }

    public String user() throws Exception {
        return getDialect().printUsernameCommand(shellControl).readStdoutOrThrow();
    }

    public String getPath() throws Exception {
        var path = shellControl
                .command(shellControl.getShellDialect().getPrintEnvironmentVariableCommand("PATH"))
                .readStdoutOrThrow();
        return path;
    }

    public String getLibraryPath() throws Exception {
        var path = shellControl
                .command(shellControl.getShellDialect().getPrintEnvironmentVariableCommand("LD_LIBRARY_PATH"))
                .readStdoutOrThrow();
        return path;
    }

    public boolean isRoot() throws Exception {
        if (shellControl.getOsType() == OsType.WINDOWS) {
            return false;
        }

        var isRoot = shellControl.executeSimpleBooleanCommand("test \"${EUID:-$(id -u)}\" -eq 0");
        return isRoot;
    }

    public Optional<FilePath> findProgram(String name) throws Exception {
        var out = shellControl
                .command(shellControl.getShellDialect().getWhichCommand(name))
                .readStdoutIfPossible();
        return out.flatMap(s -> s.lines().findFirst()).map(String::trim).map(s -> FilePath.of(s));
    }

    public void transferLocalFile(Path localPath, FilePath target) throws Exception {
        try (var in = Files.newInputStream(localPath)) {
            writeStreamFile(target, in, in.available());
        }
    }

    public boolean isInPath(String executable) throws Exception {
        return shellControl.executeSimpleBooleanCommand(
                shellControl.getShellDialect().getWhichCommand(executable));
    }

    public void cd(String directory) throws Exception {
        var d = shellControl.getShellDialect();
        var cmd = shellControl.command(d.getCdCommand(directory));
        cmd.executeAndCheck();
    }

    public String environmentVariable(String name) throws Exception {
        return shellControl
                .command(shellControl.getShellDialect().getPrintEnvironmentVariableCommand(name))
                .readStdoutOrThrow();
    }
}
